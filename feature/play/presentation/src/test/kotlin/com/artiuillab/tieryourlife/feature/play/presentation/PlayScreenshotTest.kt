package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    fun choosingCards_oneTier_offersToKeepThemInIt() = capture("play_choose_one_tier") {
        Sheet(picker.copy(page = PickerPage.Cards, chosen = setOf(1L)))
    }

    @Test
    fun choosingCards_tooMany_saysSo() = capture("play_choose_too_many") {
        Sheet(
            picker.copy(
                page = PickerPage.Cards,
                cardsFirst = true,
                groups = picker.groups.map { if (it.isPool) it.copy(count = 80) else it },
                chosen = setOf(POOL, 1L),
            ),
        )
    }

    @Test
    fun aDuel_ofTwoCardsWithoutPictures() = capture("play_duel") {
        DuelScreen(duel = duel, onPick = {}, onSkip = {}, onUndo = {}, onQuit = {})
    }

    @Test
    fun aDuel_whileRefining_dark() = capture("play_duel_refine_dark", dark = true) {
        DuelScreen(duel = duel.copy(refining = true, number = 4, total = 17, canUndo = true), onPick = {}, onSkip = {}, onUndo = {}, onQuit = {})
    }

    @Test
    fun aThinResult_saysWhichPlacesAreAGuess() = capture("play_result_thin") {
        ResultScreen(result = result, actions = ResultActions())
    }

    @Test
    fun aResult_onSomeoneElsesList_putsItInMine() = capture("play_result_mine") {
        ResultScreen(result = result.copy(thin = false, dashed = 0, refineDuels = 0, intoMine = true, tiers = result.tiers.map { tier -> tier.copy(cards = tier.cards.map { it.copy(dashed = false) }) }), actions = ResultActions())
    }

    @Test
    fun aResult_withEveryDuelSkipped_hasNothingToApply() = capture("play_result_nothing_to_apply") {
        ResultScreen(result = result.copy(duels = 0, refineDuels = 0), actions = ResultActions())
    }

    private companion object {
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

        private fun cards(first: Long, vararg titles: String, dashedFrom: Int = Int.MAX_VALUE) =
            titles.mapIndexed { at, title -> ResultCard(TierItem(first + at, title, null), dashed = at >= dashedFrom) }

        val result = ResultState(
            kind = GameKind.Hill,
            duels = 33,
            tiers = listOf(
                ResultTier(1, "S", "#B03A32", "#F1948C", cards(1, "Dune", "Arrival", "Alien")),
                ResultTier(2, "A", "#C06A25", "#E9A867", cards(10, "Solaris", "Gattaca", dashedFrom = 1)),
                ResultTier(3, "B", "#C9A227", "#E7CB6B", emptyList()),
                ResultTier(4, "C", "#3E8E5E", "#86C9A0", cards(20, "Sunshine", "Moon", "Primer", "Tenet", dashedFrom = 0)),
            ),
            dashed = 5,
            thin = true,
            refineDuels = 17,
            intoMine = false,
        )
    }
}
