package com.artiuillab.tieryourlife.feature.tier.domain.model

data class TierItem(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val source: TierItemSource = TierItemSource.MANUAL,
    /** The same on every device, unlike [id]; what a saved game names a card by. Empty where nothing keeps one. */
    val uid: String = "",
    /** Who the picture's license asks to be named, as the search found it; set once, never edited. */
    val credit: String? = null,
    /** Where the picture came from and whom it names; changes only with the picture. */
    val attribution: PictureAttribution? = null,
    /** Position in the original public snapshot, independent of local ids and placement. */
    val sourceIndex: Int? = null,
)
