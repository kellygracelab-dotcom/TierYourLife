package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import com.artiuillab.tieryourlife.feature.tier.domain.model.coverFrame
import org.junit.Assert.assertEquals
import org.junit.Test

/** What a frame shows on screen is the formula's frame: the scale and the offset agree with it. */
class FocusedImageTest {

    private val pictures = listOf(1000f to 2000f, 2000f to 600f, 1200f to 1200f, 640f to 400f)
    private val frames = listOf(IntSize(411, 200), IntSize(183, 132), IntSize(132, 132), IntSize(64, 96))
    private val focuses = listOf(null, CoverFocus(0.5f, 0.15f), CoverFocus(0f, 1f, 2f), CoverFocus(0.8f, 0.3f, 4f), CoverFocus(1f, 0f))

    @Test
    fun theDrawnCropIsTheFormulasFrame() {
        for ((width, height) in pictures) {
            for (frame in frames) {
                for (focus in focuses) {
            val scale = focusScale(focus).computeScaleFactor(Size(width, height), Size(frame.width.toFloat(), frame.height.toFloat())).scaleX
            val drawn = IntSize((width * scale).toInt(), (height * scale).toInt())
            val offset = focusAlignment(focus).align(drawn, frame, LayoutDirection.Ltr)
            val expected = coverFrame(width, height, frame.width.toFloat() / frame.height, focus)
            val case = "$width x $height in $frame at $focus"
            assertEquals(case, expected.width, frame.width / scale.toDouble(), expected.width * TOLERANCE)
            assertEquals(case, expected.left, -offset.x / scale.toDouble(), PIXEL / scale + expected.width * TOLERANCE)
            assertEquals(case, expected.top, -offset.y / scale.toDouble(), PIXEL / scale + expected.height * TOLERANCE)
        }
            }
        }
    }

    private companion object {
        const val TOLERANCE = 0.01
        const val PIXEL = 1.5
    }
}
