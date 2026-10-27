package com.artiuillab.tieryourlife.feature.account.presentation.account

import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.CropSquare
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.squareIn
import org.junit.Assert.assertEquals
import org.junit.Test

/** The square shown before the cut must be the square the cut makes. */
class FaceCropTransformationTest {

    @Test
    fun theChosenFractions_landOnTheSamePixels() {
        assertEquals(CropSquare(left = 400, top = 600, side = 1000), squareIn(4000, 3000, FaceCrop(0.1f, 0.2f, 0.25f, 1f / 3f)))
    }

    @Test
    fun aCropHangingOverTheEdge_isPulledBackInside() {
        assertEquals(CropSquare(left = 0, top = 200, side = 800), squareIn(800, 1000, FaceCrop(-0.1f, 0.9f, 1.2f, 0.96f)))
    }
}
