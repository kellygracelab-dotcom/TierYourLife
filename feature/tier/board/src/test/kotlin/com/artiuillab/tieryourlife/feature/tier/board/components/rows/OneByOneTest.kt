package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.InMemoryBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The pool ranked one card at a time: a tier's button, Skip, a card picked on the board, Undo, the end. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class OneByOneTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val moves = mutableListOf<Triple<Long, Long, Int>>()
    private val memory = InMemoryBoardLayoutMemory()
    private var list by mutableStateOf(board(poolCards = 3))

    @Test
    fun theSwitch_bringsUpThePoolsFirstCard_andATiersButtonSendsItToThatTiersEnd() {
        setBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_TOGGLE).performClick()
        assert(memory.oneCardAtATime)
        current("Card 0")

        composeRule.onNodeWithTag(TierDetailTestTags.oneByOneTier(TIER_A)).performClick()

        assertEquals(listOf(Triple(POOL_FIRST, TIER_A, 1)), moves)
        current("Card 1")
    }

    @Test
    fun skip_leavesThePoolAsItIs_andAfterTheLastCardComesBackToTheFirst() {
        memory.oneCardAtATime = true
        setBoard()

        repeat(2) { composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_SKIP).performClick() }
        current("Card 2")
        composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_SKIP).performClick()

        current("Card 0")
        assertEquals(emptyList<Triple<Long, Long, Int>>(), moves)
    }

    @Test
    fun aPlacedCardTapped_comesUpPicked_andGoesToThePoolsEnd() {
        memory.oneCardAtATime = true
        setBoard()

        composeRule.onNodeWithTag(TierDetailTestTags.tile(RANKED)).performClick()
        current("Ranked")
        composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_TO_POOL).performClick()

        assertEquals(listOf(Triple(RANKED, POOL, 3)), moves)
        current("Card 0")
    }

    @Test
    fun undo_putsThePlacedCardBackWhereItWas() {
        memory.oneCardAtATime = true
        setBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.oneByOneTier(TIER_B)).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_UNDO).performClick()

        assertEquals(listOf(Triple(POOL_FIRST, TIER_B, 0), Triple(POOL_FIRST, POOL, 0)), moves)
        current("Card 0")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theSecondDigit_sendsTheCardToTheSecondTier() {
        memory.oneCardAtATime = true
        setBoard()

        composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_PANEL).performKeyInput { pressKey(Key.Two) }

        assertEquals(listOf(Triple(POOL_FIRST, TIER_B, 0)), moves)
    }

    @Test
    @Config(qualifiers = "+land")
    fun inTheColumnBesideTheRows_theSwitchShowsThePanelInsteadOfTheCards() {
        setBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_COLUMN).assertExists()

        composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_TOGGLE).performClick()

        current("Card 0")
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST)).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.oneByOneTier(TIER_A)).performClick()
        assertEquals(listOf(Triple(POOL_FIRST, TIER_A, 1)), moves)
    }

    @Test
    fun onAPhone_theCardsNameAndWhatIsLeftStandBesideIt() {
        memory.oneCardAtATime = true
        setBoard()

        composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_NAME).assertTextEquals("Card 0")
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_COUNT, useUnmergedTree = true).assertTextEquals("3 cards left")
        composeRule.onNodeWithTag(TierDetailTestTags.tile(RANKED)).assertIsDisplayed()
    }

    @Test
    fun theTierButtons_takeAsFewEvenRowsAsSixToARowAllows() {
        assertEquals(TierButtonLayout(rows = 1, perRow = 5), tierButtonRows(5))
        assertEquals(TierButtonLayout(rows = 1, perRow = 6), tierButtonRows(6))
        assertEquals(TierButtonLayout(rows = 2, perRow = 4), tierButtonRows(7))
        assertEquals(TierButtonLayout(rows = 3, perRow = 5), tierButtonRows(13))
    }

    @Test
    fun anEmptyPool_saysEveryCardIsPlaced_andExitGoesBackToDragging() {
        memory.oneCardAtATime = true
        list = board(poolCards = 0)
        setBoard()

        composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_EXIT).performClick()

        assertFalse(memory.oneCardAtATime)
        composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_PANEL).assertDoesNotExist()
    }

    private fun current(title: String) {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TierDetailTestTags.ONE_BY_ONE_CARD).assertContentDescriptionEquals(title)
    }

    private fun setBoard() {
        composeRule.setContent {
            CompositionLocalProvider(LocalBoardLayoutMemory provides memory) {
                TierYourLifeTheme {
                    TierDetailScreenContent(
                        state = TierDetailUiState.Success(list),
                        actions = TierDetailActions(onMoveItem = ::move),
                    )
                }
            }
        }
    }

    private fun move(itemId: Long, toTierId: Long, toPosition: Int) {
        moves += Triple(itemId, toTierId, toPosition)
        val item = list.tiers.flatMap { it.items }.first { it.id == itemId }
        val without = list.tiers.map { tier -> tier.copy(items = tier.items.filterNot { it.id == itemId }) }
        list = list.copy(
            tiers = without.map { tier ->
                if (tier.id == toTierId) tier.copy(items = tier.items.toMutableList().apply { add(toPosition.coerceAtMost(size), item) }) else tier
            },
        )
    }

    private fun board(poolCards: Int) = TierList(
        id = 1,
        title = "Films",
        tiers = listOf(
            tier(TIER_A, "S", listOf(TierItem(id = RANKED, title = "Ranked", imageUrl = null))),
            tier(TIER_B, "A", emptyList()),
            tier(POOL, "Pool", List(poolCards) { TierItem(id = POOL_FIRST + it, title = "Card $it", imageUrl = null) }, isPool = true),
        ),
    )

    private fun tier(id: Long, label: String, items: List<TierItem>, isPool: Boolean = false) =
        Tier(id = id, label = label, colorLight = "#000000", colorDark = "#000000", items = items, isPool = isPool)

    private companion object {
        const val TIER_A = 1L
        const val TIER_B = 2L
        const val RANKED = 100L
        const val POOL = 6L
        const val POOL_FIRST = 200L
    }
}
