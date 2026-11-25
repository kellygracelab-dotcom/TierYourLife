package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.DISCARD_TAG
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.previewTierLists
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.components.previewTrashEntries
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Your lists (and the trash beside it) in the windows Danylo found broken:
 * sideways, where one bar with the counts in it leaves with the boards, and
 * on tablets, where the bar and the boards stand on one capped column.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class TierListsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val (width, height) = with(LocalDensity.current) { window.width.toDp() to window.height.toDp() }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalWindowShape provides WindowShape.of(width, height),
                    LocalShortWindow provides WindowShape.isShort(height),
                ) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private val boards = TierListsUiState.Success(previewTierLists, previewTierLists.size, 24)

    // Artboard 50: no lists yet, and a popular one to try.
    private val readyList = PublishedListSummary(
        id = "ready",
        title = "Eurovision winners since 2000",
        authorUid = "uid",
        authorName = "Ann",
        category = "music",
        itemCount = 25,
        updatedAtMillis = 0L,
        tierColors = listOf("#B03A32", "#C06A25", "#A98B1F", "#4E8A3E", "#3A6EA5"),
    )

    @Test
    fun noListsYet_withAReadyList() {
        var tried: String? = null
        capture("lists_empty_ready_list") {
            TierListsScreenContent(TierListsUiState.Success(emptyList(), 0, 0, readyList = readyList), onTierListClick = {}, onTryReadyList = { tried = it })
        }
        composeRule.onNodeWithTag(TierListsTestTags.READY_LIST_TRY).performClick()
        assertEquals("ready", tried)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun noListsYet_withAReadyList_onTheSmallestPhone() = capture("lists_empty_ready_list_320x640", dark = true) {
        TierListsScreenContent(TierListsUiState.Success(emptyList(), 0, 0, readyList = readyList), onTierListClick = {})
    }

    @Test
    fun lists_phonePortrait() = capture("lists_phone_portrait") { TierListsScreenContent(boards, onTierListClick = {}) }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun lists_phoneLandscape_rows() = capture("lists_landscape_rows") { TierListsScreenContent(boards, onTierListClick = {}) }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun lists_phoneLandscape_rows_dark() = capture("lists_landscape_rows_dark", dark = true) {
        TierListsScreenContent(boards, onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun lists_phoneLandscape_grid() = capture("lists_landscape_grid") {
        TierListsScreenContent(boards.copy(asPictures = true), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun lists_phoneLandscape_grid_dark() = capture("lists_landscape_grid_dark", dark = true) {
        TierListsScreenContent(boards.copy(asPictures = true), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun lists_phoneLandscape_search() = capture("lists_landscape_search") {
        TierListsScreenContent(
            boards.copy(lists = previewTierLists.take(2), mode = HomeMode.Searching("fi")),
            onTierListClick = {},
        )
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-mdpi")
    fun lists_tabletPortrait() = capture("lists_tablet_portrait") { TierListsScreenContent(boards, onTierListClick = {}) }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-mdpi")
    fun lists_tabletPortrait_grid() = capture("lists_tablet_portrait_grid") {
        TierListsScreenContent(boards.copy(asPictures = true), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun trash_phoneLandscape() = capture("trash_landscape") {
        TrashScreenContent(state = TrashUiState.Success(previewTrashEntries), onBack = {})
    }

    private val begun = listOf(
        StartedList("films", "Every A24 film", placed = 14, total = 22, gameSaved = true, section = "film_tv"),
        StartedList("games", "PS2 era", placed = 3, total = 40),
    )

    @Test
    fun continue_phonePortrait() = capture("lists_continue_phone_portrait") {
        TierListsScreenContent(boards.copy(started = begun), onTierListClick = {})
    }

    @Test
    fun continue_phonePortrait_dark() = capture("lists_continue_phone_portrait_dark", dark = true) {
        TierListsScreenContent(boards.copy(started = begun), onTierListClick = {})
    }

    // One favourite: only it wears a star, in pictures as in rows.
    private val starred = boards.copy(
        lists = previewTierLists.mapIndexed { index, list -> if (index == 1) list.copy(favouritedAt = 1L) else list },
    )

    @Test
    fun continue_phonePortrait_pictures() = capture("lists_continue_phone_portrait_pictures") {
        TierListsScreenContent(starred.copy(started = begun, asPictures = true), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun continue_phoneLandscape_grid() = capture("lists_continue_landscape_grid") {
        TierListsScreenContent(starred.copy(started = begun, asPictures = true), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun continue_phoneLandscape_rows_dark() = capture("lists_continue_landscape_rows_dark", dark = true) {
        TierListsScreenContent(starred.copy(started = begun), onTierListClick = {})
    }

    @Test
    fun selection_phonePortrait_pictures() = capture("lists_selection_phone_portrait_pictures") {
        val ticked = setOf(previewTierLists[0].id, previewTierLists[1].id)
        TierListsScreenContent(starred.copy(asPictures = true, mode = HomeMode.Selecting(ticked)), onTierListClick = {})
    }

    @Test
    fun continue_noBoardsOfOnesOwn() = capture("lists_continue_no_boards") {
        TierListsScreenContent(TierListsUiState.Success(emptyList(), 0, 24, started = begun), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-mdpi")
    fun continue_tabletPortrait() = capture("lists_continue_tablet_portrait") {
        TierListsScreenContent(boards.copy(started = begun), onTierListClick = {})
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-mdpi")
    fun continue_tabletPortrait_dark() = capture("lists_continue_tablet_portrait_dark", dark = true) {
        TierListsScreenContent(boards.copy(started = begun), onTierListClick = {})
    }

    @Test
    fun continue_opensTheList_orItsGame() {
        val opened = mutableListOf<Pair<String, Boolean>>()
        composeRule.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(boards.copy(started = begun), onTierListClick = {}, onContinueStarted = { id, game -> opened += id to game })
            }
        }
        composeRule.onNodeWithTag(TierListsTestTags.continueList("games")).performClick()
        composeRule.onNodeWithTag(TierListsTestTags.resumeGame("films")).performClick()
        composeRule.onNodeWithTag(TierListsTestTags.resumeGame("games")).assertDoesNotExist()

        assertEquals(listOf("games" to false, "films" to true), opened)
    }

    @Test
    fun aBegunList_goesBySwipeEitherWay_orByItsMenu() {
        val discarded = mutableListOf<String>()
        composeRule.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(boards.copy(started = begun), onTierListClick = {}, onDiscardStarted = { discarded += it })
            }
        }
        composeRule.onNodeWithTag(TierListsTestTags.started("games")).performTouchInput { swipeRight() }
        composeRule.onNodeWithTag(TierListsTestTags.started("films")).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        assertEquals(listOf("games", "films"), discarded)
    }

    // The phone's case: the only begun list. Its block leaves the grid when it goes and comes back with Undo,
    // and the row must come back as a row, not as one swiped away that goes again at once.
    @Test
    fun undo_bringsTheOnlyBegunListBack_toStay() {
        val discarded = mutableListOf<String>()
        composeRule.setContent {
            var started by remember { mutableStateOf(begun.take(1)) }
            TierYourLifeTheme {
                TierListsScreenContent(
                    boards.copy(started = started),
                    onTierListClick = {},
                    onDiscardStarted = { id ->
                        discarded += id
                        started = started.filterNot { it.listId == id }
                    },
                    onUndoDiscardStarted = { started = begun.take(1) },
                )
            }
        }
        composeRule.onNodeWithTag(TierListsTestTags.started("films")).performTouchInput { swipeRight() }
        composeRule.onNodeWithTag(TierListsTestTags.CONTINUE_BLOCK).assertDoesNotExist()
        composeRule.onNodeWithText("Undo").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TierListsTestTags.started("films")).assertExists()
        assertEquals(listOf("films"), discarded)
    }

    @Test
    fun aShortSwipe_putsTheRowBack() {
        val discarded = mutableListOf<String>()
        composeRule.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(boards.copy(started = begun), onTierListClick = {}, onDiscardStarted = { discarded += it })
            }
        }
        composeRule.onNodeWithTag(TierListsTestTags.started("games")).performTouchInput {
            // Slowly: a fling goes whatever the distance.
            swipeRight(startX = centerX, endX = centerX + width / 6f, durationMillis = 1_000)
        }
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), discarded)
    }

    @Test
    fun aLongPress_opensTheMenu_whoseLastItemDiscards() {
        val discarded = mutableListOf<String>()
        composeRule.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(boards.copy(started = begun), onTierListClick = {}, onDiscardStarted = { discarded += it })
            }
        }
        composeRule.onNodeWithTag(TierListsTestTags.started("films")).performTouchInput { longClick() }
        // The row's own button and the menu's item.
        composeRule.onAllNodesWithText("Resume game").assertCountEquals(2)
        composeRule.onNodeWithTag(TierListsTestTags.started("films") + DISCARD_TAG).performClick()
        composeRule.onNodeWithText("Your version discarded").assertExists()
        assertEquals(listOf("films"), discarded)
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
