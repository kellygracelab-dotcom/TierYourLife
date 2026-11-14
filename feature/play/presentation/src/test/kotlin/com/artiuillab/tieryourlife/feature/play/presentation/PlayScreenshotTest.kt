package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Play's screens as they look, compared with the pictures beside the tests.
 * A change that moves a pixel on purpose re-records them
 * (`recordRoborazziDebug`); one that moves them by accident fails
 * `verifyRoborazziDebug` in CI. The picker is drawn as the sheet's content:
 * the sheet itself is a window of its own.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class PlayScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent { TierYourLifeTheme(darkTheme = dark) { content() } }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Composable
    private fun Sheet(picker: Picker) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(top = 24.dp),
        ) { PickerContent(picker, PickerActions()) }
    }

    @Test
    fun thePicker_withASavedGame_onSomeoneElsesList() = capture("play_picker") {
        Sheet(picker.copy(resume = ResumeOffer(GameKind.Swiss, played = 48, total = 136, startedAt = System.currentTimeMillis() - DAY), intoMine = true))
    }

    @Test
    fun thePicker_dark() = capture("play_picker_dark", dark = true) {
        Sheet(picker.copy(kind = GameKind.Hill))
    }

    @Test
    fun choosingCards_oneTier_ordersOnlyThatTier() = capture("play_choose_one_tier") {
        Sheet(picker.copy(page = PickerPage.Cards, chosen = setOf(1L)))
    }

    @Test
    fun choosingCards_allNinetyOneAndQuickSamples() = capture("play_choose_all_91") {
        val board = board(1L to (1L..91L).toList(), POOL to emptyList())
        Sheet(Picker(board.title, groupsOf(board), setOf(1L), cards = cardsOf(board), page = PickerPage.Cards))
    }

    @Test
    fun individualCards_showTheActualSelectedComposition() = capture("play_individual_selection") {
        val board = board(1L to (1L..91L).toList(), POOL to emptyList())
        Surface(Modifier.fillMaxSize()) {
            Column {
                PickerContent(Picker(board.title, groupsOf(board), setOf(1L), cards = cardsOf(board), selectedIds = (1L..16L).toList(), page = PickerPage.Individual), PickerActions())
            }
        }
    }

    @Test
    fun aDuel_ofTwoCardsWithoutPictures() = capture("play_duel") {
        DuelScreen(duel = duel, onPick = {}, onSkip = {}, onUndo = {}, onQuit = {})
    }

    @Test
    fun aDuel_whileRefining_dark() = capture("play_duel_refine_dark", dark = true) {
        DuelScreen(duel = duel.copy(refining = true, number = 4, total = 17, canUndo = true), onPick = {}, onSkip = {}, onUndo = {}, onQuit = {})
    }

    // A Galaxy Z Flip6 held sideways: wide, and short of height.

    /** The dialog as AdaptiveSheet frames it, over the dimmed board: the dialog itself is a window of its own. */
    @Composable
    private fun WideDialog(picker: Picker) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .background(Color.Black.copy(alpha = 0.32f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .heightIn(max = 363.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column { PickerContent(picker, PickerActions(), wide = true) }
            }
        }
    }

    private val savedGame = ResumeOffer(GameKind.Swiss, played = 48, total = 136, startedAt = System.currentTimeMillis() - DAY)

    @Test
    @Config(qualifiers = FLIP_LANDSCAPE)
    fun landscape_thePicker_threeGamesSideBySide() = capture("play_landscape_picker") {
        WideDialog(picker.copy(resume = savedGame))
    }

    @Test
    @Config(qualifiers = FLIP_LANDSCAPE)
    fun landscape_thePicker_dark() = capture("play_landscape_picker_dark", dark = true) {
        WideDialog(picker.copy(resume = savedGame))
    }

    @Test
    @Config(qualifiers = FLIP_LANDSCAPE)
    fun landscape_choosingCards_inTwoColumns() = capture("play_landscape_choose") {
        WideDialog(manyTiers.copy(page = PickerPage.Cards))
    }

    @Test
    @Config(qualifiers = FLIP_LANDSCAPE)
    fun landscape_choosingCards_dark() = capture("play_landscape_choose_dark", dark = true) {
        WideDialog(manyTiers.copy(page = PickerPage.Cards))
    }

    @Test
    @Config(qualifiers = FLIP_LANDSCAPE)
    fun landscape_aDuel_skipBetweenTheCards() = capture("play_landscape_duel") {
        DuelScreen(duel = pictureDuel, onPick = {}, onSkip = {}, onUndo = {}, onQuit = {})
    }

    @Test
    @Config(qualifiers = FLIP_LANDSCAPE)
    fun landscape_aDuel_dark() = capture("play_landscape_duel_dark", dark = true) {
        DuelScreen(duel = pictureDuel, onPick = {}, onSkip = {}, onUndo = {}, onQuit = {})
    }

    private companion object {
        const val FLIP_LANDSCAPE = "w930dp-h411dp-land-xhdpi"

        const val DAY = 24 * 60 * 60 * 1000L

        // Text antialiasing differs a little between the Windows that records
        // and the Linux that verifies; a real change moves far more than this.
        val OPTIONS = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F),
        )

        val picker = Picker(
            title = "Sci-fi films",
            groups = listOf(
                CardGroup(POOL, "Unranked", null, "#DAD7E0", "#46464F", count = 22, isPool = true),
                CardGroup(1L, "S", "Masterpieces", "#B03A32", "#F1948C", count = 6, isPool = false),
                CardGroup(2L, "A", null, "#C06A25", "#E9A867", count = 6, isPool = false),
            ),
            chosen = setOf(POOL, 1L, 2L),
        )

        val duel = DuelState(
            kind = GameKind.Swiss,
            first = TierItem(1, "Dune: Part Two", null),
            second = TierItem(2, "Everything Everywhere All at Once", null),
            number = 2,
            total = 136,
            round = 1,
            rounds = 8,
            canUndo = true,
        )

        val manyTiers = Picker(
            title = "Sci-fi films",
            groups = listOf(
                CardGroup(POOL, "Unranked", null, "#DAD7E0", "#46464F", count = 22, isPool = true),
                CardGroup(1L, "S", "Masterpiece", "#B03A32", "#F1948C", count = 142, isPool = false),
                CardGroup(2L, "A", "Great", "#C06A25", "#E9A867", count = 27, isPool = false),
                CardGroup(3L, "B", "Good", "#C9A227", "#E7CB6B", count = 64, isPool = false),
                CardGroup(4L, "C", "Watchable", "#3E8E5E", "#86C9A0", count = 89, isPool = false),
                CardGroup(5L, "D", "No", "#2F6FA8", "#8DB8E3", count = 102, isPool = false),
            ),
            chosen = setOf(POOL, 2L),
        )

        // The first card has a picture, which Robolectric never loads: its name stands under it.
        val pictureDuel = duel.copy(
            first = TierItem(1, "Annihilation", "https://example.invalid/annihilation.jpg"),
            second = TierItem(2, "Coherence", null),
            number = 49,
            round = 3,
        )
    }
}
