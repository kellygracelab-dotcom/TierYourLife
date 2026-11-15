package com.artiuillab.tieryourlife.feature.tier.presentation.coveradjust

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The step after a single picture is chosen (Danylo, 15.11: "the head gets
 * cut off"): a tall portrait with its face near the top, moved up under the
 * frame and a little closer, in every window shape and both themes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp-xhdpi")
class AdjustCoverScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val raised = AdjustCoverUiState(picture = "portrait", focus = CoverFocus(0.5f, 0.2f, 1.3f), loaded = true)

    /** A portrait in a frame of sky, with a face a fifth of the way down: what a centre crop cuts the head off. */
    private val portrait: BitmapPainter by lazy {
        val picture = ImageBitmap(PICTURE_WIDTH, PICTURE_HEIGHT)
        Canvas(picture).apply {
            val sky = Paint().apply {
                shader = LinearGradientShader(Offset.Zero, Offset(0f, PICTURE_HEIGHT.toFloat()), listOf(Color(0xFF7FB2E5), Color(0xFF2E5E3A)))
            }
            drawRect(0f, 0f, PICTURE_WIDTH.toFloat(), PICTURE_HEIGHT.toFloat(), sky)
            drawRect(PICTURE_WIDTH * 0.3f, PICTURE_HEIGHT * 0.3f, PICTURE_WIDTH * 0.7f, PICTURE_HEIGHT.toFloat(), Paint().apply { color = Color(0xFF8A4B2A) })
            drawCircle(Offset(PICTURE_WIDTH / 2f, PICTURE_HEIGHT * 0.2f), PICTURE_WIDTH * 0.16f, Paint().apply { color = Color(0xFFF2C29B) })
            drawCircle(Offset(PICTURE_WIDTH * 0.44f, PICTURE_HEIGHT * 0.19f), PICTURE_WIDTH * 0.02f, Paint().apply { color = Color(0xFF222222) })
            drawCircle(Offset(PICTURE_WIDTH * 0.56f, PICTURE_HEIGHT * 0.19f), PICTURE_WIDTH * 0.02f, Paint().apply { color = Color(0xFF222222) })
        }
        BitmapPainter(picture)
    }

    private fun capture(name: String, state: AdjustCoverUiState = raised, dark: Boolean = false) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val (shape, short) = with(LocalDensity.current) {
                WindowShape.of(window.width.toDp(), window.height.toDp()) to WindowShape.isShort(window.height.toDp())
            }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalWindowShape provides shape, LocalShortWindow provides short) {
                    AdjustCoverContent(state = state, painter = portrait, onMove = {}, onReset = {}, onDone = {}, onClose = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Test
    fun light() {
        capture("adjust_cover_light")

        composeRule.onNodeWithTag(AdjustCoverTestTags.RESET).assertIsEnabled()
        composeRule.onNodeWithTag(AdjustCoverTestTags.FEED_PREVIEW).assertExists()
        composeRule.onNodeWithTag(AdjustCoverTestTags.HEADER_PREVIEW).assertExists()
    }

    @Test
    fun dark() = capture("adjust_cover_dark", dark = true)

    @Test
    fun centred_hasNothingToReset() {
        capture("adjust_cover_centred", raised.copy(focus = CoverFocus.CENTRED))

        composeRule.onNodeWithTag(AdjustCoverTestTags.RESET).assertIsNotEnabled()
    }

    @Test
    @Config(qualifiers = "en-w891dp-h411dp-land-xhdpi")
    fun phoneLandscape() = capture("adjust_cover_landscape")

    @Test
    @Config(qualifiers = "en-w1280dp-h800dp-land-mdpi")
    fun tablet() = capture("adjust_cover_tablet", dark = true)

    @Test
    @Config(qualifiers = "en-w360dp-h380dp-xhdpi")
    fun flipCover() = capture("adjust_cover_flip_cover")

    @Test
    @Config(qualifiers = "en-w360dp-h780dp-xhdpi")
    fun flipOpen() = capture("adjust_cover_flip_open", dark = true)

    private companion object {
        const val PICTURE_WIDTH = 800
        const val PICTURE_HEIGHT = 1400
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
