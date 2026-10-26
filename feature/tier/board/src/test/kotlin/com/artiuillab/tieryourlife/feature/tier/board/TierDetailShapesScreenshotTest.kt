package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The board in the shapes that used to squeeze it: a phone held sideways, a
 * tablet either way up, a Flip's cover. Each test names its window; the
 * window's shape is what the app would hand the screen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class TierDetailShapesScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(
        name: String,
        shape: WindowShape,
        readOnly: Boolean = false,
        dark: Boolean = false,
        allLists: List<TierList> = emptyList(),
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalWindowShape provides shape) {
                TierYourLifeTheme(darkTheme = dark) {
                    TierDetailScreenContent(
                        allLists = allLists,
                        state = TierDetailUiState.Success(previewTierList),
                        readOnly = readOnly,
                        onReaderMoreClick = if (readOnly) ({}) else null,
                        barExtras = if (readOnly) ({ mode -> ReaderStandIn(mode) }) else null,
                        titleSubline = if (readOnly) ({ Text("Artiman · Film & TV ›", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }) else null,
                        poolNote = if (readOnly) ({ Text("Someone else's list. Rank it however you like.", style = MaterialTheme.typography.bodySmall) }) else null,
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun ownBoard_phoneLandscape() = capture("board_phone_landscape", WindowShape.of(914.dp, 411.dp))

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun readersBoard_phoneLandscape() = capture("board_reader_phone_landscape", WindowShape.of(914.dp, 411.dp), readOnly = true)

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun ownBoard_phoneLandscape_dark() = capture("board_phone_landscape_dark", WindowShape.of(914.dp, 411.dp), dark = true)

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun ownBoard_tabletLandscape() = capture("board_tablet_landscape", WindowShape.of(1280.dp, 800.dp))

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun readersBoard_tabletLandscape() = capture("board_reader_tablet_landscape", WindowShape.of(1280.dp, 800.dp), readOnly = true)

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun ownBoard_tabletLandscape_besideYourLists() =
        capture("board_tablet_landscape_index", WindowShape.of(1280.dp, 800.dp), allLists = yourLists)

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun ownBoard_tabletLandscape_besideYourLists_dark() =
        capture("board_tablet_landscape_index_dark", WindowShape.of(1280.dp, 800.dp), dark = true, allLists = yourLists)

    @Test
    @Config(qualifiers = TABLET_PORTRAIT)
    fun ownBoard_tabletPortrait() = capture("board_tablet_portrait", WindowShape.of(800.dp, 1280.dp))

    @Test
    @Config(qualifiers = TABLET_PORTRAIT)
    fun readersBoard_tabletPortrait() = capture("board_reader_tablet_portrait", WindowShape.of(800.dp, 1280.dp), readOnly = true)

    @Test
    @Config(qualifiers = FLIP_COVER)
    fun readersBoard_flipCover() = capture("board_reader_flip_cover", WindowShape.of(360.dp, 380.dp), readOnly = true)

    private val yourLists = listOf(
        previewTierList,
        previewTierList.copy(id = 2, title = "Board games"),
        previewTierList.copy(id = 3, title = "Coffee in town"),
        previewTierList.copy(id = 4, title = "Albums of 2025"),
    )

    private companion object {
        const val PHONE_LANDSCAPE = "w914dp-h411dp-normal-long-notround-land-420dpi-keyshidden-nonav"
        const val TABLET_LANDSCAPE = "w1280dp-h800dp-xlarge-long-notround-land-xhdpi-keyshidden-nonav"
        const val TABLET_PORTRAIT = "w800dp-h1280dp-xlarge-long-notround-port-xhdpi-keyshidden-nonav"
        const val FLIP_COVER = "w360dp-h380dp-small-notlong-notround-port-xhdpi-keyshidden-nonav"

        // Text antialiasing differs a little between the Windows that records
        // and the Linux that verifies; a real change moves far more than this.
        val OPTIONS = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F),
        )
    }
}

/** What a community list puts in the bar, drawn with plain parts: the board's layout is under test, not the reader's screen. */
@Composable
private fun RowScope.ReaderStandIn(mode: BoardBar) {
    if (mode == BoardBar.Compact) {
        AssistChip(onClick = {}, label = { Text("Artiman") })
    }
    SingleChoiceSegmentedButtonRow(Modifier.height(if (mode == BoardBar.Docked) 40.dp else 32.dp)) {
        SegmentedButton(selected = true, onClick = {}, shape = SegmentedButtonDefaults.itemShape(0, 2), icon = {}) { Text("Theirs") }
        SegmentedButton(selected = false, onClick = {}, shape = SegmentedButtonDefaults.itemShape(1, 2), icon = {}) { Text("Mine") }
    }
    Button(onClick = {}, modifier = Modifier.height(36.dp)) {
        Text("Save to my lists", style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
