package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.tips.InMemoryTipMemory
import com.artiuillab.tieryourlife.core.theme.tips.LocalTipMemory
import com.artiuillab.tieryourlife.core.theme.tips.Tip
import com.artiuillab.tieryourlife.core.theme.tips.TipTestTags
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The newcomer's three tips on a board (artboards 47–50): when each shows, what closes it, and how it looks. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = BoardTipsTest.PHONE_PORTRAIT)
class BoardTipsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun board(placed: Int, inPool: Int): TierList {
        val cards = (1L..(placed + inPool).toLong()).map { TierItem(id = it, title = "Card $it", imageUrl = null) }
        return TierList(
            id = 1,
            title = "Sci-fi films",
            tiers = listOf(
                Tier(id = 10, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = cards.take(placed)),
                Tier(id = 20, label = "A", colorLight = "#C06A25", colorDark = "#E9A867", items = emptyList()),
                Tier(id = 30, label = "B", colorLight = "#A98B1F", colorDark = "#D8C05A", items = emptyList()),
                Tier(id = POOL, label = "Pool", colorLight = "#000000", colorDark = "#000000", items = cards.drop(placed), isPool = true),
            ),
        )
    }

    private fun show(list: TierList, memory: InMemoryTipMemory, actions: TierDetailActions = TierDetailActions(), dark: Boolean = false) {
        composeRule.setContent {
            CompositionLocalProvider(LocalTipMemory provides memory) {
                TierYourLifeTheme(darkTheme = dark) {
                    TierDetailScreenContent(state = TierDetailUiState.Success(list), actions = actions)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun tips(vararg seen: Tip) = InMemoryTipMemory(off = false, seen = seen.toSet())

    @Test
    fun whichTip_followsWhatThePersonHasDone() {
        val fresh = tips()
        assertEquals(Tip.Place, fresh.tipFor(board(placed = 0, inPool = 10), readOnly = false, canAdd = true, canPlay = true))
        assertEquals(Tip.Add, fresh.tipFor(board(placed = 0, inPool = 0), readOnly = false, canAdd = true, canPlay = true))
        assertNull("someone else's list has no Add", fresh.tipFor(board(placed = 0, inPool = 0), readOnly = false, canAdd = false, canPlay = true))
        assertNull("not before the first card", fresh.tipFor(board(placed = 2, inPool = 8), readOnly = false, canAdd = true, canPlay = true))

        val placedOne = tips(Tip.Place)
        assertEquals(Tip.Play, placedOne.tipFor(board(placed = 1, inPool = 7), readOnly = false, canAdd = true, canPlay = true))
        assertNull("under eight cards", placedOne.tipFor(board(placed = 1, inPool = 6), readOnly = false, canAdd = true, canPlay = true))
        assertNull("no Play here", placedOne.tipFor(board(placed = 1, inPool = 9), readOnly = false, canAdd = true, canPlay = false))
        assertNull(tips(Tip.Place, Tip.Play).tipFor(board(placed = 1, inPool = 9), readOnly = false, canAdd = true, canPlay = true))

        assertNull("the author's ranking, read", fresh.tipFor(board(placed = 0, inPool = 10), readOnly = true, canAdd = false, canPlay = true))
        assertNull("no more tips", InMemoryTipMemory(off = true).tipFor(board(placed = 0, inPool = 10), readOnly = false, canAdd = true, canPlay = true))
    }

    @Test
    fun placeTip_ringsTheFirstCard_andGotItPutsItAwayForGood() {
        val memory = tips()
        show(board(placed = 0, inPool = 10), memory)

        composeRule.onNodeWithTag(TipTestTags.BUBBLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TipTestTags.RING).assertIsDisplayed()
        composeRule.onNodeWithTag(TipTestTags.GOT_IT).performClick()

        composeRule.onNodeWithTag(TipTestTags.BUBBLE).assertDoesNotExist()
        assertTrue(memory.seen(Tip.Place))
        assertFalse(memory.off)
    }

    @Test
    fun noMoreTips_turnsThemAllOff() {
        val memory = tips()
        show(board(placed = 0, inPool = 10), memory)

        composeRule.onNodeWithTag(TipTestTags.NO_MORE).performClick()

        composeRule.onNodeWithTag(TipTestTags.BUBBLE).assertDoesNotExist()
        assertTrue(memory.off)
    }

    @Test
    fun aTapElsewhere_closesTheTip_andStillDoesWhatItWasFor() {
        val memory = tips()
        var played = 0
        show(board(placed = 0, inPool = 10), memory, TierDetailActions(onPlay = { played++ }))

        composeRule.onNodeWithTag(TierDetailTestTags.PLAY).performClick()

        composeRule.onNodeWithTag(TipTestTags.BUBBLE).assertDoesNotExist()
        assertEquals(1, played)
        assertTrue(memory.seen(Tip.Place))
    }

    @Test
    fun doingIt_isAsGoodAsBeingTold() {
        val memory = tips()
        val moved = mutableListOf<Long>()
        var heard: TierDetailActions? = null
        val list = board(placed = 0, inPool = 10)
        composeRule.setContent {
            CompositionLocalProvider(LocalTipMemory provides memory) {
                TierYourLifeTheme {
                    BoardTips(list, TierDetailActions(onMoveItem = { id, _, _ -> moved += id }, onPlay = {}), readOnly = false) { heard = it }
                }
            }
        }
        composeRule.waitForIdle()
        val actions = heard!!

        actions.onMoveItem(1, POOL, 0)
        assertFalse("back to the pool is not placing", memory.seen(Tip.Place))
        actions.onMoveItem(1, 10, 0)
        assertTrue(memory.seen(Tip.Place))
        actions.onPlay!!()
        assertTrue(memory.seen(Tip.Play))
        actions.onAddClick!!()
        assertTrue(memory.seen(Tip.Add))
        assertEquals(listOf(1L, 1L), moved)
    }

    @Test
    fun withTheTipsOff_theBoardIsAsItWas() {
        show(board(placed = 0, inPool = 10), InMemoryTipMemory())

        composeRule.onNodeWithTag(TipTestTags.BUBBLE).assertDoesNotExist()
    }

    @Test
    fun placeTip_light() = capture("tip_place_light", board(placed = 0, inPool = 10), tips())

    @Test
    fun placeTip_dark() = capture("tip_place_dark", board(placed = 0, inPool = 10), tips(), dark = true)

    @Test
    fun playTip_light() = capture("tip_play_light", board(placed = 1, inPool = 24), tips(Tip.Place), TierDetailActions(onPlay = {}))

    @Test
    fun addTip_light() = capture("tip_add_light", board(placed = 0, inPool = 0), tips())

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE)
    fun placeTip_besideTheColumn_landscape() = capture("tip_place_landscape", board(placed = 0, inPool = 10), tips())

    @Test
    @Config(qualifiers = CI_EMULATOR)
    fun placeTip_onTheSmallestPhone() = capture("tip_place_320x640", board(placed = 0, inPool = 10), tips())

    private fun capture(name: String, list: TierList, memory: InMemoryTipMemory, actions: TierDetailActions = TierDetailActions(), dark: Boolean = false) {
        show(list, memory, actions, dark)
        composeRule.onNodeWithTag(TipTestTags.BUBBLE).assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    companion object {
        private const val POOL = 99L
        const val PHONE_PORTRAIT = "w411dp-h914dp-normal-long-notround-port-420dpi-keyshidden-nonav"
        const val PHONE_LANDSCAPE = "w914dp-h411dp-normal-long-notround-land-420dpi-keyshidden-nonav"
        const val CI_EMULATOR = "w320dp-h640dp-normal-notlong-notround-port-mdpi-keyshidden-nonav"

        // Text antialiasing differs a little between the Windows that records and the Linux that verifies.
        private val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
