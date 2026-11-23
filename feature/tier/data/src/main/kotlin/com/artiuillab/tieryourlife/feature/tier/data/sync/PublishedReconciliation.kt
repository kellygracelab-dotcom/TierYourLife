package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList

/** The ids to drop from the boards, and whose each remaining one is. */
internal data class PublishedReconciliation(
    val forget: Set<String>,
    val owners: Map<String, String>,
)

/**
 * Which published ids on this phone are gone. Only the account that published
 * a list can say so: one missing from somebody else's account is not gone,
 * it was never theirs. An id nobody has claimed yet belongs to the account
 * signed in when it was last checked; a switch since then hands it to that
 * account rather than letting the new one drop it.
 */
internal fun reconcilePublished(
    claimed: Set<String>,
    inAccount: Set<String>,
    account: String,
    lastAccount: String?,
    owners: Map<String, String>,
): PublishedReconciliation {
    val unclaimed = claimed - inAccount - owners.keys
    val switchedFrom = lastAccount?.takeIf { it != account }
    val known = owners.filterKeys { it in claimed } +
        switchedFrom?.let { previous -> unclaimed.associateWith { previous } }.orEmpty() +
        (inAccount intersect claimed).associateWith { account }
    val forget = claimed.filterTo(mutableSetOf()) { id -> id !in inAccount && (known[id] ?: account) == account }
    return PublishedReconciliation(forget = forget, owners = known - forget)
}

/**
 * Whether a board's cards are no longer laid out as its published list's:
 * cards per tier with the pool last, as the server counts them, or the total
 * when the server did not say. Equal counts prove nothing, so they say no.
 */
internal fun cardsDifferFromPublished(
    boardCounts: List<Int>,
    publishedCounts: List<Int>,
    publishedTotal: Int,
): Boolean = if (publishedCounts.isEmpty()) boardCounts.sum() != publishedTotal else boardCounts != publishedCounts

/** A card as its published list holds it: its tier among the ranked ones, null in the pool; its name; its picture. */
internal data class ListedCard(val tier: Int?, val title: String, val picture: String?)

/**
 * Whether the published list still shows the board: the title, the tiers, and
 * every card in its place with its name and picture. What cannot be told for
 * sure, a list from before cards' places were kept, says no.
 */
internal fun sameAsPublished(title: String, tiers: List<String>, cards: List<ListedCard>, list: PublishedList): Boolean {
    if (list.arrangement.size != list.items.size || cards.size != list.items.size) return false
    val listed = list.items.mapIndexed { index, item -> ListedCard(list.arrangement[index], item.title, item.imageUrl) }
    return title == list.summary.title &&
        tiers == list.tiers.map { it.label } &&
        cards.zip(listed).all { (here, there) -> here.tier == there.tier && here.title == there.title && samePicture(here.picture, there.picture) }
}

/** An address as it is; this phone's own picture by its name, which the list's copy of it keeps in its address. */
private fun samePicture(here: String?, there: String?): Boolean =
    here == there || (here != null && there != null && !here.startsWith("https://") && here in there.substringBefore('?'))

/**
 * Boards that lost track of their list, by board uid, and the list each gets
 * back: a publish the server finished but whose answer never arrived. Only a
 * list no board here claims, and only one per board.
 */
internal fun relinkPublished(
    unlinked: Set<String>,
    inAccount: Map<String, String?>,
    claimed: Set<String>,
): Map<String, String> = inAccount.entries
    .mapNotNull { (id, boardUid) -> boardUid?.takeIf { it in unlinked && id !in claimed }?.let { it to id } }
    .groupBy({ it.first }, { it.second })
    .filterValues { it.size == 1 }
    .mapValues { it.value.single() }
