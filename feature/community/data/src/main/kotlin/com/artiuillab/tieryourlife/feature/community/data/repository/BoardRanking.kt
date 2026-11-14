package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedItemDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.RankingDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ShowRankingRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.toDto
import com.artiuillab.tieryourlife.feature.community.data.remote.proxyCode
import com.artiuillab.tieryourlife.feature.community.data.remote.rankingRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures

private const val MAX_ADDED_CARDS = 50

/**
 * A saved copy as its reader's ranking of the list it came from (SPEC
 * list-flow К3): the list's own cards by their place in it, the cards the
 * reader added after them, at N+k, in the order they first appear.
 */
internal suspend fun TierList.rankingRequest(listCards: Int, listTiers: Int, version: Long?, images: OwnPictures): ShowRankingRequestDto {
    val ranked = tiers.filterNot { it.isPool }
    fun TierItem.listIndex(): Int? = sourceIndex?.takeIf { it in 0 until listCards }
    val added = tiers.flatMap { it.items }.filter { it.listIndex() == null }
    val unfit = when {
        ranked.size != listTiers -> PublishError.RankInstead
        added.size > MAX_ADDED_CARDS -> PublishError.TooLarge
        else -> null
    }
    if (unfit != null) throw PublishRefused(unfit)
    val addedCards = added.sent(images)
    val addedAt = added.withIndex().associate { (k, card) -> card.id to listCards + k }
    val rows = ranked.map { tier -> tier.items.map { card -> card.listIndex() ?: addedAt.getValue(card.id) } }
    val tierOf = ranked.withIndex().flatMap { (at, tier) -> tier.items.mapNotNull { card -> card.listIndex()?.let { it to at } } }.toMap()
    return ShowRankingRequestDto(
        arrangement = (0 until listCards).map { tierOf[it] },
        rows = rows,
        tierLabels = ranked.map { it.label },
        version = version,
        addedCards = addedCards.ifEmpty { null },
    )
}

/** The cards as the server takes them, their own pictures sent first. */
private suspend fun List<TierItem>.sent(images: OwnPictures): List<PublishedItemDto> {
    val wanted = mapNotNull { images.pictureIdOf(it.imageUrl) ?: privatePictureIdOf(it.imageUrl) }.distinct()
    val uploaded = images.sendNow(wanted)
    if (wanted.any { it !in uploaded }) throw PublishRefused(PublishError.PictureRefused)
    return map { card ->
        PublishedItemDto(
            title = card.title,
            imageUrl = webPictureOf(card.imageUrl),
            pictureId = images.pictureIdOf(card.imageUrl) ?: privatePictureIdOf(card.imageUrl),
            credit = card.credit,
            attribution = card.attribution?.toDto(),
        )
    }
}

/**
 * Mine from the list's page never loses the cards this reader added: from
 * their saved board of the list, in the tiers they have there, or else the
 * ones their ranking already has, where it has them. The list's own cards
 * keep the places Mine gives them.
 */
internal suspend fun ShowRankingRequestDto.keepingAdded(board: TierList?, mine: suspend () -> RankingDto?, images: OwnPictures): ShowRankingRequestDto =
    if (board != null) withAddedFrom(board, images) else withAddedOf(mine())

private suspend fun ShowRankingRequestDto.withAddedFrom(board: TierList, images: OwnPictures): ShowRankingRequestDto {
    val listCards = arrangement.size
    fun TierItem.added() = sourceIndex?.takeIf { it in 0 until listCards } == null
    val placed = board.tiers.filterNot { it.isPool }.withIndex().flatMap { (at, tier) -> tier.items.filter { it.added() }.map { it to at } }
    val waiting = board.tiers.filter { it.isPool }.flatMap { it.items }.filter { it.added() }.map { it to null }
    val added = (placed + waiting).take(MAX_ADDED_CARDS)
    val tierOf = added.withIndex().groupBy({ it.value.second }, { listCards + it.index })
    return if (added.isEmpty()) {
        this
    } else {
        copy(rows = rows.mapIndexed { at, row -> row + tierOf[at].orEmpty() }, addedCards = added.map { it.first }.sent(images))
    }
}

private fun ShowRankingRequestDto.withAddedOf(ranking: RankingDto?): ShowRankingRequestDto {
    val kept = ranking?.takeIf { it.addedCards.isNotEmpty() } ?: return this
    val keptRows = kept.allRows.ifEmpty { kept.rows }
    return copy(
        rows = rows.mapIndexed { at, row -> row + keptRows.getOrNull(at).orEmpty().filter { it >= arrangement.size } },
        addedCards = kept.addedCards,
    )
}

/** This reader's own ranking of [listId], read through the pages until it turns up. */
internal suspend fun CommunityApi.myRanking(listId: String): RankingDto? {
    var after: String? = null
    var found: RankingDto? = null
    var pages = 0
    do {
        val page = rankings(listId, after)
        found = page.rankings.firstOrNull { it.mine }
        after = page.nextCursor
        pages++
    } while (found == null && after != null && pages < MAX_RANKING_PAGES)
    return found
}

private const val MAX_RANKING_PAGES = 40

/** A refused board ranking in the board's words. */
internal fun Throwable.boardRankingError(): PublishError = when {
    this is PublishRefused -> error
    proxyCode() == "PICTURE_REFUSED" -> PublishError.PictureRefused
    else -> when (rankingRefusal()) {
        RankingRefusal.NotSignedIn -> PublishError.NotSignedIn
        RankingRefusal.Banned -> PublishError.Banned(null)
        RankingRefusal.ListGone -> PublishError.ListGone
        RankingRefusal.ListChanged -> PublishError.SourceChanged
        RankingRefusal.TooManyWebPictures -> PublishError.TooManyWebPictures
        RankingRefusal.WebPicturesToday -> PublishError.WebPicturesToday
        RankingRefusal.OwnList, RankingRefusal.BadTierNames, RankingRefusal.CardsDontMatch, RankingRefusal.NotAccepted -> PublishError.RankInstead
        RankingRefusal.Failed -> PublishError.Unknown
    }
}
