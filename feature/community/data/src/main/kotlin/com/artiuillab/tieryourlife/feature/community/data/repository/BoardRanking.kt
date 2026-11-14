package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedItemDto
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
    val wanted = added.mapNotNull { images.pictureIdOf(it.imageUrl) ?: privatePictureIdOf(it.imageUrl) }.distinct()
    val uploaded = images.sendNow(wanted)
    if (wanted.any { it !in uploaded }) throw PublishRefused(PublishError.PictureRefused)
    val addedAt = added.withIndex().associate { (k, card) -> card.id to listCards + k }
    val rows = ranked.map { tier -> tier.items.map { card -> card.listIndex() ?: addedAt.getValue(card.id) } }
    val tierOf = ranked.withIndex().flatMap { (at, tier) -> tier.items.mapNotNull { card -> card.listIndex()?.let { it to at } } }.toMap()
    return ShowRankingRequestDto(
        arrangement = (0 until listCards).map { tierOf[it] },
        rows = rows,
        tierLabels = ranked.map { it.label },
        version = version,
        addedCards = added.map { card ->
            PublishedItemDto(
                title = card.title,
                imageUrl = webPictureOf(card.imageUrl),
                pictureId = images.pictureIdOf(card.imageUrl) ?: privatePictureIdOf(card.imageUrl),
                credit = card.credit,
                attribution = card.attribution?.toDto(),
            )
        }.ifEmpty { null },
    )
}

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
