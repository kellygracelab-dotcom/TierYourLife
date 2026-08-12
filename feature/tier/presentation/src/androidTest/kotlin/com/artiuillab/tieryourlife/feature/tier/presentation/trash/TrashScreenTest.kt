package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrashScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_composesNoMoreVertButton() {
        setScreen(TrashUiState.Success(emptyList()))

        composeRule.onNodeWithTag(TrashTestTags.EMPTY_STATE).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.trash_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(TrashTestTags.MORE).assertDoesNotExist()
    }

    @Test
    fun nonEmptyState_showsTitleAndMoreVertButton() {
        setScreen(TrashUiState.Success(listOf(deletedList(1L, "Pizza in Lisbon", 5))))

        composeRule.onNodeWithText(string(R.string.trash_title)).assertIsDisplayed()
        composeRule.onNodeWithTag(TrashTestTags.MORE).assertIsDisplayed()
    }

    @Test
    fun removeDialog_appearsOnRemoveClick_andCancelDismissesWithoutRemoving() {
        val entry = deletedList(1L, "Pizza in Lisbon", 5)
        var removed = false
        setScreen(TrashUiState.Success(listOf(entry)), onRemoveListPermanently = { removed = true })

        composeRule.onNodeWithTag(TrashTestTags.removeButton(entry)).performClick()

        composeRule.onNodeWithTag(TrashTestTags.REMOVE_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.remove_dialog_title)).assertIsDisplayed()

        composeRule.onNodeWithTag(TrashTestTags.REMOVE_CANCEL).performClick()

        composeRule.onNodeWithTag(TrashTestTags.REMOVE_DIALOG).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(false, removed) }
    }

    @Test
    fun removeDialog_confirmInvokesPermanentRemoval() {
        val entry = deletedList(1L, "Pizza in Lisbon", 5)
        var removedId: Long? = null
        setScreen(TrashUiState.Success(listOf(entry)), onRemoveListPermanently = { removedId = it })

        composeRule.onNodeWithTag(TrashTestTags.removeButton(entry)).performClick()
        composeRule.onNodeWithTag(TrashTestTags.REMOVE_CONFIRM).performClick()

        composeRule.onNodeWithTag(TrashTestTags.REMOVE_DIALOG).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1L, removedId) }
    }

    @Test
    fun emptyTrashDialog_appearsFromTheMenu_andCancelDismissesWithoutEmptying() {
        var emptied = false
        setScreen(
            TrashUiState.Success(listOf(deletedList(1L, "Pizza in Lisbon", 5))),
            onEmptyTrash = { emptied = true },
        )

        composeRule.onNodeWithTag(TrashTestTags.MORE).performClick()
        composeRule.onNodeWithTag(TrashTestTags.MENU_EMPTY_TRASH).performClick()

        composeRule.onNodeWithTag(TrashTestTags.EMPTY_TRASH_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.empty_trash_dialog_title)).assertIsDisplayed()

        composeRule.onNodeWithTag(TrashTestTags.EMPTY_TRASH_CANCEL).performClick()

        composeRule.onNodeWithTag(TrashTestTags.EMPTY_TRASH_DIALOG).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(false, emptied) }
    }

    @Test
    fun emptyTrashDialog_confirmInvokesEmptyTrash() {
        var emptied = false
        setScreen(
            TrashUiState.Success(listOf(deletedList(1L, "Pizza in Lisbon", 5))),
            onEmptyTrash = { emptied = true },
        )

        composeRule.onNodeWithTag(TrashTestTags.MORE).performClick()
        composeRule.onNodeWithTag(TrashTestTags.MENU_EMPTY_TRASH).performClick()
        composeRule.onNodeWithTag(TrashTestTags.EMPTY_TRASH_CONFIRM).performClick()

        composeRule.runOnIdle { assertEquals(true, emptied) }
    }

    @Test
    fun restore_neverShowsAConfirmationDialog() {
        val entry = deletedList(1L, "Pizza in Lisbon", 5)
        var restoredId: Long? = null
        setScreen(TrashUiState.Success(listOf(entry)), onRestoreList = { restoredId = it })

        composeRule.onNodeWithTag(TrashTestTags.restoreButton(entry)).performClick()

        composeRule.onNodeWithTag(TrashTestTags.REMOVE_DIALOG).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1L, restoredId) }
    }

    private fun setScreen(
        state: TrashUiState,
        onRestoreList: (Long) -> Unit = {},
        onRestoreItem: (Long) -> Unit = {},
        onRemoveListPermanently: (Long) -> Unit = {},
        onRemoveItemPermanently: (Long) -> Unit = {},
        onEmptyTrash: () -> Unit = {},
    ) {
        composeRule.setContent {
            TierYourLifeTheme {
                TrashScreenContent(
                    state = state,
                    onBack = {},
                    onRestoreList = onRestoreList,
                    onRestoreItem = onRestoreItem,
                    onRemoveListPermanently = onRemoveListPermanently,
                    onRemoveItemPermanently = onRemoveItemPermanently,
                    onEmptyTrash = onEmptyTrash,
                )
            }
        }
    }

    private fun string(resourceId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)

    private fun deletedList(id: Long, title: String, itemCount: Int): TrashEntry.DeletedList =
        TrashEntry.DeletedList(id = id, title = title, itemCount = itemCount, deletedAtMillis = 0L)
}
