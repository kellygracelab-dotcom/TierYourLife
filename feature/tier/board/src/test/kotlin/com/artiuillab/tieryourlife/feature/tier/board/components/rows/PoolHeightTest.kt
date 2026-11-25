package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailActions
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailScreenContent
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Danylo 25.11: "the bottom panel keeps jumping". A phone's pool is one line of
 * cards or folded, and stays the same height whatever the number of cards in it,
 * down to none, and whatever its count says.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w360dp-h740dp-mdpi")
class PoolHeightTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val cards = mutableStateOf(40)
    private lateinit var density: Density

    @Test
    fun openPool_keepsItsHeight_asCardsLeaveIt_downToNone() {
        setBoard(pictures = false)
        val heights = listOf(40, 12, 10, 9, 2, 1, 0).map { heightWith(it) }

        assertEquals(heights.map { heights.first() }, heights)
    }

    @Test
    fun openPool_withPictures_keepsItsHeight_too() {
        setBoard(pictures = true)
        val heights = listOf(40, 9, 1, 0).map { heightWith(it) }

        assertEquals(heights.map { heights.first() }, heights)
    }

    @Test
    fun openPool_keepsItsHeight_whileATappedCardWaitsForItsTier() {
        setBoard(pictures = false)
        val before = heightWith(40)
        composeRule.onNodeWithTag(TierDetailTestTags.tile(200)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tap a tier", substring = true).assertExists()

        assertEquals(before, heightWith(40))
    }

    @Test
    fun foldedPool_keepsItsHeight_whetherItsGlimpseHasPicturesOrNot() {
        setBoard(pictures = true)
        // The handle, above the head's chips.
        composeRule.onNodeWithTag(TierDetailTestTags.POOL_TOGGLE).performTouchInput { click(Offset(centerX, 4f)) }
        val heights = listOf(40, 9, 1, 0).map { heightWith(it) }

        assertEquals(heights.map { heights.first() }, heights)
    }

    @Test
    fun theHeadIsSizedForEveryCountOfAsManyDigits() {
        assertEquals((0..9).toList() + 9, countsOfAsManyDigits(0))
        assertEquals((10..35).toList() + 99, countsOfAsManyDigits(42))
        assertEquals((100..125).toList() + 999, countsOfAsManyDigits(100))
    }

    private fun heightWith(count: Int): Float {
        cards.value = count
        composeRule.waitForIdle()
        return composeRule.onNodeWithTag(TierDetailTestTags.POOL_PANEL).fetchSemanticsNode().boundsInRoot.height / density.density
    }

    private fun setBoard(pictures: Boolean) {
        composeRule.setContent {
            density = LocalDensity.current
            val pool = List(cards.value) { TierItem(id = 200L + it, title = "Card $it", imageUrl = if (pictures) "file:///card$it.png" else null) }
            val list = TierList(
                id = 1,
                title = "Films",
                tiers = listOf(
                    tier(1, "S", listOf(TierItem(id = 100, title = "Top", imageUrl = null))),
                    tier(POOL, "Pool", pool, isPool = true),
                ),
            )
            TierYourLifeTheme {
                TierDetailScreenContent(state = TierDetailUiState.Success(list), actions = TierDetailActions())
            }
        }
        composeRule.waitForIdle()
    }

    private fun tier(id: Long, label: String, items: List<TierItem>, isPool: Boolean = false) =
        Tier(id = id, label = label, colorLight = "#000000", colorDark = "#000000", items = items, isPool = isPool)

    private companion object {
        const val POOL = 6L
    }
}
