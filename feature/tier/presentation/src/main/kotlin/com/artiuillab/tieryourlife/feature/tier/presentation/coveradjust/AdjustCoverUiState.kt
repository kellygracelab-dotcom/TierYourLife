package com.artiuillab.tieryourlife.feature.tier.presentation.coveradjust

import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus

/**
 * The board's single cover and the part of it chosen so far; nothing is kept
 * until Done. A null [focus] is the picture's own default: the centre, or a
 * little above it on a tall picture.
 */
data class AdjustCoverUiState(
    val picture: String? = null,
    val focus: CoverFocus? = null,
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    /** A board with no single cover has nothing to adjust: a collage or the cards' own art. */
    val nothingToAdjust: Boolean get() = loaded && picture == null
}
