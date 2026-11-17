package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.InMemoryBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.MoveItemSheetContent
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.summary
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
        oneByOne: Boolean = false,
        mine: Boolean = false,
    ) {
        val memory = InMemoryBoardLayoutMemory().apply { oneCardAtATime = oneByOne }
        // A reader arranging Mine: the board moves, the bar is still the reader's.
        val reader = readOnly || mine
        composeRule.setContent {
            CompositionLocalProvider(LocalWindowShape provides shape, LocalBoardLayoutMemory provides memory) {
                TierYourLifeTheme(darkTheme = dark) {
                    TierDetailScreenContent(
                        allLists = allLists.map { it.summary() },
                        state = TierDetailUiState.Success(previewTierList),
                        actions = TierDetailActions(onPlay = if (reader) ({}) else null),
                        readOnly = readOnly,
                        subtitle = if (reader) "Film & TV · 12 cards" else null,
                        onReaderMoreClick = if (reader) ({}) else null,
                        barExtras = if (reader) ({ mode -> ReaderStandIn(mode) }) else null,
                        titleSubline = if (reader) ({ Text("Artiman · Film & TV ›", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }) else null,
                        poolNote = if (reader) ({ Text("Someone else's list. Rank it however you like.", style = MaterialTheme.typography.bodySmall) }) else null,
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

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun oneByOne_phoneLandscape() =
        capture("board_phone_landscape_one_by_one", WindowShape.of(914.dp, 411.dp), oneByOne = true)

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun oneByOne_readersBoard_phoneLandscape_dark() =
        capture("board_reader_phone_landscape_one_by_one_dark", WindowShape.of(914.dp, 411.dp), dark = true, oneByOne = true, mine = true)

    @Test
    @Config(qualifiers = PHONE_PORTRAIT)
    fun readersBoard_phonePortrait_theWholeName() =
        capture("board_reader_phone_portrait", WindowShape.of(411.dp, 914.dp), readOnly = true)

    @Test
    @Config(qualifiers = "ru-$PHONE_PORTRAIT", fontScale = 1.15f)
    fun ownBoard_phonePortrait_russian_largerText() = capture("board_phone_portrait_ru", WindowShape.of(411.dp, 914.dp))

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun cardSheet_phoneLandscape() = cardSheet("card_sheet_phone_landscape", dark = false)

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun cardSheet_phoneLandscape_dark() = cardSheet("card_sheet_phone_landscape_dark", dark = true)

    /** The sheet as a dialog frames it in a window held sideways: 360 high and 400 wide at most. */
    private fun cardSheet(name: String, dark: Boolean) {
        val ranked = previewTierList.tiers.filterNot { it.isPool }
        composeRule.setContent {
            CompositionLocalProvider(LocalWindowShape provides WindowShape.of(914.dp, 411.dp), LocalShortWindow provides true) {
                TierYourLifeTheme(darkTheme = dark) {
                    Surface(
                        modifier = Modifier.widthIn(max = 400.dp).heightIn(max = 363.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Column {
                            MoveItemSheetContent(
                                item = ranked.first().items.first(),
                                currentTierId = ranked.first().id,
                                rankedTiers = ranked,
                                pool = previewTierList.tiers.first { it.isPool },
                                onMoveToTier = { _, _ -> },
                                onRemove = {},
                                onAdjustPicture = {},
                            )
                        }
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private val yourLists = listOf(
        previewTierList,
        previewTierList.copy(id = 2, title = "Board games"),
        previewTierList.copy(id = 3, title = "Coffee in town"),
        previewTierList.copy(id = 4, title = "Albums of 2025"),
    )

    private companion object {
        const val PHONE_LANDSCAPE = "w914dp-h411dp-normal-long-notround-land-420dpi-keyshidden-nonav"
        const val PHONE_PORTRAIT = "w411dp-h914dp-normal-long-notround-port-420dpi-keyshidden-nonav"
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
