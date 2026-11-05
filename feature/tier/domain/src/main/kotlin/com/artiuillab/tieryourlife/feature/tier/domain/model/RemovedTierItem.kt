package com.artiuillab.tieryourlife.feature.tier.domain.model

/** A card taken off its board for good, kept only while Undo can still put it back where it was. */
data class RemovedTierItem(
    val id: Long,
    val uid: String,
    val tierListId: Long,
    val tierId: Long,
    val position: Int,
    val title: String,
    val imageUrl: String?,
    val source: TierItemSource,
    val credit: String? = null,
    /** The board's cover before this card took its picture off it; null where the cover did not change. */
    val coverBefore: List<String>? = null,
)
