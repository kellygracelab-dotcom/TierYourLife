package com.artiuillab.tieryourlife.feature.community.presentation.following

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FollowingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyoneFollowed_isListedByName_andTheNamelessAreStillThere() {
        setScreen(
            FollowingUiState.Ready(
                listOf(
                    FollowedAuthor(uid = "a", name = "Olena M.", photoUrl = null),
                    FollowedAuthor(uid = "gone", name = "", photoUrl = null),
                ),
            ),
        )

        composeRule.onNodeWithText("Olena M.").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.community_author_unnamed)).assertIsDisplayed()
    }

    @Test
    fun thePersonsButton_asksForThatPerson() {
        var toggled: String? = null
        setScreen(
            FollowingUiState.Ready(listOf(FollowedAuthor(uid = "a", name = "Olena M.", photoUrl = null))),
            onToggle = { toggled = it },
        )

        composeRule.onNodeWithTag(FollowingTestTags.toggle(FollowKeys.person("a"))).performClick()

        assertEquals(FollowKeys.person("a"), toggled)
    }

    @Test
    fun followingNobody_saysSo() {
        setScreen(FollowingUiState.Ready(emptyList()))

        composeRule.onNodeWithTag(FollowingTestTags.EMPTY).assertIsDisplayed()
    }

    private fun setScreen(state: FollowingUiState, onToggle: (String) -> Unit = {}) {
        composeRule.setContent {
            TierYourLifeTheme {
                FollowingScreenContent(
                    state = state,
                    onBack = {},
                    onOpenAuthor = { _, _, _ -> },
                    onToggle = onToggle,
                    onRetry = {},
                )
            }
        }
    }

    private fun string(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
