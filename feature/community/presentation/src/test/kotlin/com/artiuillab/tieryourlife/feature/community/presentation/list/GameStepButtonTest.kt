package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** QA Н3 and Н4 on the list's page: the save's words on a reader's placing, and a game's Undo that stays. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp-xhdpi")
class GameStepButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val board = TierList(
        id = 0,
        title = "Pizza styles",
        tiers = listOf(
            Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = listOf(TierItem(1, "Neapolitan", null))),
            Tier(id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", isPool = true, items = listOf(TierItem(2, "Detroit", null))),
        ),
    )
    private val afterGame = CommunityListUiState.Success(
        list = board,
        authorName = "Olena M.",
        authorUid = "olena",
        showing = Showing.Mine,
        arranged = true,
        gameStep = GameStep.Undo,
    )

    private var turns = 0

    private fun show(state: CommunityListUiState.Success) {
        composeRule.setContent {
            TierYourLifeTheme {
                CommunityListScreenContent(
                    state = state,
                    onBack = {},
                    onMoveItem = { _, _, _ -> },
                    onSave = {},
                    onRetry = {},
                    onTurnGame = { turns++ },
                )
            }
        }
    }

    @Test
    fun afterAGame_thePageHasItsOwnUndo() {
        show(afterGame)

        composeRule.onNodeWithTag(CommunityListTestTags.GAME_STEP).assertTextEquals("Undo").performClick()

        composeRule.runOnIdle { assertEquals(1, turns) }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/list_after_game_undo.png", roborazziOptions = OPTIONS)
    }

    @Test
    fun beforeTheGame_itSaysRedo() {
        show(afterGame.copy(showing = Showing.Theirs, arranged = false, gameStep = GameStep.Redo))

        composeRule.onNodeWithTag(CommunityListTestTags.GAME_STEP).assertTextEquals("Redo")
    }

    @Test
    fun noGame_noButton() {
        show(afterGame.copy(gameStep = null))

        composeRule.onNodeWithTag(CommunityListTestTags.GAME_STEP).assertDoesNotExist()
    }

    @Test
    fun aReadersPlacing_savesAsMyList() {
        val danylo = Ranking("r1", "Danylo Petrov", null, arrangement = listOf(0, null), tierLabels = listOf("S"), updatedAtMillis = 0)
        show(afterGame.copy(showing = Showing.Theirs, chosenRanking = danylo, gameStep = null))

        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertTextEquals("Save my list")
    }

    @Test
    fun theAuthorsPlacing_savesTheAuthorsList() {
        show(afterGame.copy(showing = Showing.Theirs, gameStep = null))

        composeRule.onNodeWithTag(CommunityListTestTags.SAVE).assertTextEquals("Save author’s list")
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))
    }
}
