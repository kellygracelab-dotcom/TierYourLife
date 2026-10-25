package com.artiuillab.tieryourlife.feature.tier.presentation.coverpicker

import com.artiuillab.tieryourlife.feature.tier.board.components.MOSAIC_CELLS

enum class CoverPickerTab { Cards, Search }

/**
 * The board's own pictures to choose from, and the choice as it stands. One
 * picture is the cover by itself, two to six make a collage, none leaves the
 * cover to the cards, as the feed draws a list today.
 */
data class CoverPickerUiState(
    val title: String = "",
    /** This board's web pictures, each once, in board order. */
    val candidates: List<String> = emptyList(),
    /** What a collage shows while nothing is chosen. */
    val automatic: List<String> = emptyList(),
    val tierColors: List<String> = emptyList(),
    /** In the order they were tapped, never more than a mosaic holds. */
    val chosen: List<String> = emptyList(),
    val tab: CoverPickerTab = CoverPickerTab.Cards,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val coverImageUrl: String? get() = chosen.singleOrNull()

    val previewImages: List<String> get() = if (chosen.size > 1) chosen else automatic

    val canChooseMore: Boolean get() = chosen.size < MOSAIC_CELLS
}
