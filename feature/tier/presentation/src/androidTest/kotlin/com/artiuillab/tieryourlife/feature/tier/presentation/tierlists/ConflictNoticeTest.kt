package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Two phones changed the same board, so both versions are on screen and the
 * person has to be able to tell which is which.
 */
@RunWith(AndroidJUnit4::class)
class ConflictNoticeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val dismissed = mutableListOf<String>()

    private val minutesAgo = System.currentTimeMillis() - 10 * 60 * 1000
    private val yesterday = System.currentTimeMillis() - 26 * 60 * 60 * 1000

    @Test
    fun theNotice_saysWhatHappenedAndWhatToDo() {
        setScreen(twoVersions())

        composeRule.onNodeWithTag(TierListsTestTags.CONFLICT_BANNER).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.conflict_title, "Sci-fi films")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.conflict_body)).assertIsDisplayed()
    }

    @Test
    fun theCopyCarriesTheNameOfThePhoneItCameFrom() {
        setScreen(twoVersions())

        composeRule.onNodeWithText(string(R.string.conflict_tag, "Pixel 7")).assertIsDisplayed()
    }

    // The name only helps when it is a human one. Times are what actually
    // separate the two, and they are on both cards.
    @Test
    fun bothCardsSayWhenTheyWereLastTouched() {
        setScreen(twoVersions())

        composeRule.onNodeWithText(ago(minutesAgo, here = true)).assertIsDisplayed()
        composeRule.onNodeWithText(ago(yesterday, here = false)).assertIsDisplayed()
    }

    @Test
    fun gotIt_reachesTheCaller() {
        setScreen(twoVersions())

        composeRule.onNodeWithTag(TierListsTestTags.CONFLICT_GOT_IT).performClick()

        composeRule.runOnIdle { assertEquals(listOf("Sci-fi films"), dismissed) }
    }

    // An ordinary board is one board. Nothing about it is a comparison, so it
    // carries neither a tag nor a time.
    @Test
    fun aBoardWithNoTwin_saysNothingExtra() {
        setScreen(listOf(board(1, "Sci-fi films")))

        composeRule.onNodeWithTag(TierListsTestTags.CONFLICT_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(TierListsTestTags.CONFLICT_BANNER).assertDoesNotExist()
    }

    private fun twoVersions() = listOf(
        board(1, "Sci-fi films", editedAt = minutesAgo, hasTwin = true),
        board(2, "Sci-fi films", editedAt = yesterday, arrivedFrom = "Pixel 7", hasTwin = true),
    )

    private fun setScreen(lists: List<TierList>) {
        dismissed.clear()
        composeRule.setContent {
            TierYourLifeTheme {
                TierListsScreenContent(
                    state = TierListsUiState.Success(
                        lists = lists,
                        totalListCount = lists.size,
                        rankedCount = lists.size,
                        conflict = lists.firstOrNull { it.arrivedFrom != null && it.hasTwin },
                    ),
                    onTierListClick = {},
                    onDismissConflictNotice = { dismissed += it },
                )
            }
        }
    }

    private fun board(
        id: Long,
        title: String,
        editedAt: Long? = null,
        arrivedFrom: String? = null,
        hasTwin: Boolean = false,
    ) = TierList(
        id = id,
        title = title,
        tiers = listOf(
            Tier(
                id = id * 10,
                label = "S",
                colorLight = "#B03A32",
                colorDark = "#F1948C",
                items = listOf(TierItem(id = id * 100, title = "Arrival", imageUrl = null)),
            ),
        ),
        arrivedFrom = arrivedFrom,
        editedAt = editedAt,
        hasTwin = hasTwin,
    )

    private fun ago(atMs: Long, here: Boolean): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val span = android.text.format.DateUtils.getRelativeTimeSpanString(
            atMs,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS,
        ).toString()
        return if (here) {
            context.getString(R.string.conflict_edited_here, span)
        } else {
            context.getString(R.string.conflict_edited_there, "Pixel 7", span)
        }
    }

    private fun string(resourceId: Int, vararg formatArgs: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId, *formatArgs)
}
