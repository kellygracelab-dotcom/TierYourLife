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
    /** Which part of the picture the card shows, by the covers' formula; null for the picture's default. */
    val focus: CoverFocus? = null,
)

/**
 * A card with a picture may have no name. The words older apps and the site
 * filled in for one ("Untitled card") are read as no name; a card without a
 * picture keeps them, since it has nothing else to show.
 */
fun cardTitleAsRead(title: String, hasPicture: Boolean): String = if (hasPicture && title == OLD_UNNAMED_CARD) "" else title

private const val OLD_UNNAMED_CARD = "Untitled card"
