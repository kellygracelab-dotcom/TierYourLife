package com.artiuillab.tieryourlife.feature.tier.board.components.sheets

import org.junit.Assert.assertEquals
import org.junit.Test

/** B in the light theme is gold since 18.11.2026 (artboard 56); a list made before still has the olive. */
class TierBGoldTest {

    @Test
    fun theOldOlive_isStillTheBPreset_soThePickerShowsItChosen() {
        val gold = tierColorSelectionFor("#B88A00", "#D8C05A")
        val olive = tierColorSelectionFor("#A98B1F", "#D8C05A")

        assertEquals(TierColorSelection.Preset(2, "B88A00", "D8C05A"), gold)
        assertEquals(gold, olive)
    }
}
