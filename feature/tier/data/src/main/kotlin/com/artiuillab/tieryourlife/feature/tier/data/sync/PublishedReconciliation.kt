package com.artiuillab.tieryourlife.feature.tier.data.sync

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
