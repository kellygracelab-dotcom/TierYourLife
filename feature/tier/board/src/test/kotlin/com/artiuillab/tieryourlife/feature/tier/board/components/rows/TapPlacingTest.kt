package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** "Start here: tap a card, then the tier it belongs in" on an ordinary board (QA D1, 19.11), as on the site. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class TapPlacingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val moves = mutableListOf<Triple<Long, Long, Int>>()
    private var list by mutableStateOf(board())

    @Test
    fun aCardTapped_thenATiersRow_goesToThatTiersEnd() {
        setBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(TIER_B)).performClick()

        assertEquals(listOf(Triple(POOL_FIRST, TIER_B, 0)), moves)
    }

    @Test
    fun aCardTapped_thenATiersBand_goesThere_ratherThanEditingIt() {
        setBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(TIER_A)).performClick()

        assertEquals(listOf(Triple(POOL_FIRST, TIER_A, 1)), moves)
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SHEET).assertDoesNotExist()
    }

    @Test
    fun nothingPicked_aBandStillEditsTheTier_andACardTappedTwiceIsLetGo() {
        setBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST)).performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(DOUBLE_TAP_GAP)
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(TIER_A)).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SHEET).assertIsDisplayed()
        assertEquals(emptyList<Triple<Long, Long, Int>>(), moves)
    }

    @Test
    fun aCardOnTheBoard_movesFromItsTierToAnother() {
        setBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(RANKED)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(TIER_B)).performClick()

        assertEquals(listOf(Triple(RANKED, TIER_B, 0)), moves)
    }

    // The Designer, 19.11: the bottom panel says what to do next, a tap on another card moves the pick, Back lets go.
    @Test
    fun whileACardIsPicked_thePanelSaysTapATier_anotherCardTakesThePick_andBackLetsGo() {
        setBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_COUNT, useUnmergedTree = true).assertTextEquals("Tap a tier")

        composeRule.mainClock.advanceTimeBy(DOUBLE_TAP_GAP)
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST + 1)).performClick()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(TIER_B)).performClick()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SHEET).assertIsDisplayed()
        assertEquals(emptyList<Triple<Long, Long, Int>>(), moves)
    }

    @Test
    fun aTapOnAnotherCard_movesThePickToIt() {
        setBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST)).performClick()
        composeRule.mainClock.advanceTimeBy(DOUBLE_TAP_GAP)
        composeRule.onNodeWithTag(TierDetailTestTags.tile(POOL_FIRST + 1)).performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.tierRow(TIER_B)).performClick()

        assertEquals(listOf(Triple(POOL_FIRST + 1, TIER_B, 0)), moves)
    }

    private fun setBoard() {
        composeRule.setContent {
            CompositionLocalProvider(LocalBoardLayoutMemory provides InMemoryBoardLayoutMemory()) {
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

    private fun board() = TierList(
        id = 1,
        title = "Films",
        tiers = listOf(
            tier(TIER_A, "S", listOf(TierItem(id = RANKED, title = "Ranked", imageUrl = null))),
            tier(TIER_B, "A", emptyList()),
            tier(POOL, "Pool", List(3) { TierItem(id = POOL_FIRST + it, title = "Card $it", imageUrl = null) }, isPool = true),
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
        const val DOUBLE_TAP_GAP = 1_000L
    }
}
