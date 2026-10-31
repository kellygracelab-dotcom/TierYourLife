package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TIER_COLOR_PRESETS
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** Far below any card the server numbers, so a blank card never shares an id with a real one. */
private const val BLANK_ID_BASE = -1_000L

/**
 * The page as the tapped card already knew it: the real header, the
 * author's rows in their colours, and a blank card for every card in the
 * tier it sits in. The answer replaces the blanks where they stand, so nothing
 * moves. Null when the card did not say how the tiers fill.
 */
internal fun ListPreview.firstFrame(listId: String, signedIn: Boolean): CommunityListUiState.Success? {
    if (tierCounts.isEmpty()) return null
    var made = 0L
    fun blanks(count: Int) = List(count.coerceAtLeast(0)) { TierItem(id = BLANK_ID_BASE - made++, title = "", imageUrl = null) }
    val ranked = tierCounts.dropLast(1).mapIndexed { index, count ->
        val light = tierColors.getOrNull(index)
        Tier(
            id = index.toLong(),
            label = "",
            colorLight = light ?: POOL_LIGHT,
            colorDark = light?.let(::darkFor) ?: POOL_DARK,
            items = blanks(count),
        )
    }
    val placed = ranked + pool(blanks(tierCounts.last()))
    val board = TierList(id = 0, title = title, tiers = placed, authorName = authorName.orEmpty())
    val cards = placed.flatMap { it.items }
    return CommunityListUiState.Success(
        list = board,
        mine = board.copy(tiers = ranked.map { it.copy(items = emptyList()) } + pool(cards)),
        showing = if (cards.isEmpty() && !mine) Showing.Mine else Showing.Theirs,
        authorName = authorName.orEmpty(),
        authorUid = authorUid,
        authorPhotoUrl = authorPhotoUrl,
        editorial = authorName == null,
        byNobody = byNobody,
        follow = viewerFollowsAuthor?.let { FollowState(following = it) },
        rankingCount = rankingCount,
        basedOn = basedOn,
        section = section,
        own = mine,
        anonymous = anonymous,
        listId = listId,
        signedIn = signedIn,
        arriving = true,
    )
}

/**
 * A summary carries each tier's light colour only. The app's own palettes
 * say what the dark one is; a colour of somebody's own keeps its light one
 * until the list lands.
 */
internal fun darkFor(light: String): String {
    val key = light.removePrefix("#").uppercase()
    val dark = DARK_OF[key] ?: return light
    return if (light.startsWith("#")) "#$dark" else dark
}

private val DARK_OF: Map<String, String> =
    TIER_COLOR_PRESETS.associate { it.colorLight.uppercase() to it.colorDark.uppercase() } +
        // A new board's own five, which a published list carries most often.
        mapOf("B03A32" to "F1948C", "C06A25" to "E9A867", "A98B1F" to "D8C05A", "3F7F55" to "7FC393", "3C6E99" to "86B8DE")

private const val POOL_LIGHT = "#DAD7E0"
private const val POOL_DARK = "#46464F"
