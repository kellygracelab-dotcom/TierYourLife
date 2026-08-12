package com.artiuillab.tieryourlife.feature.tier.presentation.common

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

// Pixel capture requires API 26; the icons themselves still support minSdk 24.
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class VectorIconMirroringTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aDirectionalIcon_pointsTheOtherWayInRightToLeft() {
        showBothDirections { BackIcon() }

        val leftToRight = horizontalCentreOfInk(LTR_TAG)
        val rightToLeft = horizontalCentreOfInk(RTL_TAG)

        assertTrue("expected the arrowhead left of centre, got $leftToRight", leftToRight < 0.45f)
        assertTrue("expected the arrowhead right of centre, got $rightToLeft", rightToLeft > 0.55f)
        assertTrue(abs((1f - leftToRight) - rightToLeft) < 0.02f)
    }

    @Test
    fun aSymmetricIcon_isLeftAloneInRightToLeft() {
        showBothDirections { PlusIcon(24.dp, Color.Black) }

        assertTrue(abs(horizontalCentreOfInk(LTR_TAG) - horizontalCentreOfInk(RTL_TAG)) < 0.02f)
    }

    private fun showBothDirections(icon: @Composable () -> Unit) {
        composeRule.setContent {
            TierYourLifeTheme {
                Column {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(Modifier.testTag(LTR_TAG)) { icon() }
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Box(Modifier.testTag(RTL_TAG)) { icon() }
                    }
                }
            }
        }
    }

    private fun horizontalCentreOfInk(tag: String): Float {
        val bitmap = composeRule.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val background = pixels[0]

        var weighted = 0.0
        var total = 0.0
        pixels.forEachIndexed { index, pixel ->
            val ink = colourDistance(pixel, background)
            if (ink > 0) {
                weighted += ink * (index % bitmap.width).toDouble()
                total += ink.toDouble()
            }
        }
        return (weighted / total / (bitmap.width - 1)).toFloat()
    }

    private fun colourDistance(pixel: Int, background: Int): Int =
        abs(((pixel shr 16) and 0xFF) - ((background shr 16) and 0xFF)) +
            abs(((pixel shr 8) and 0xFF) - ((background shr 8) and 0xFF)) +
            abs((pixel and 0xFF) - (background and 0xFF))

    private companion object {
        const val LTR_TAG = "icon_ltr"
        const val RTL_TAG = "icon_rtl"
    }
}
