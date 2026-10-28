package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.KnownSections
import com.artiuillab.tieryourlife.feature.tier.board.common.LocalSections
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.CategorySheet
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierListSettingsScreenContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A section this person proposed is theirs to publish into while it waits, and only theirs. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class WaitingSectionChoiceTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val streetFood = Section(id = "street-food", name = "Street food", status = SectionStatus.Pending, fileUnder = "food", mine = true)
    private val sections = KnownSections(SectionDirectory(sections = Section.BUILT_IN + streetFood, fetchedAtMillis = 1L))

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent {
            TierYourLifeTheme { CompositionLocalProvider(LocalSections provides sections) { content() } }
        }
    }

    @Test
    fun thePicker_offersTheirWaitingSection_andEndsInProposingOne() {
        val chosen = mutableListOf<String>()
        var proposing = 0
        show { CategorySheet(selected = "food", onDismiss = {}, onSelect = { chosen += it }, withWaiting = true, onPropose = { proposing++ }) }

        composeRule.onNodeWithText("Waiting for a moderator").assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.categoryRow("street-food")).performScrollTo().performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.PROPOSE_SECTION).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("street-food"), chosen)
            assertEquals(1, proposing)
        }
    }

    @Test
    fun withoutWaiting_thePickerShowsOnlyOpenSections() {
        show { CategorySheet(selected = "food", onDismiss = {}, onSelect = {}) }

        composeRule.onNodeWithTag(TierDetailTestTags.categoryRow("street-food")).assertDoesNotExist()
        composeRule.onNodeWithTag(TierDetailTestTags.PROPOSE_SECTION).assertDoesNotExist()
    }

    @Test
    fun aListFiledUnderTheirWaitingSection_saysWhereItIsGoing() {
        show {
            TierListSettingsScreenContent(
                list = previewTierList.copy(category = "street-food"),
                onBack = {},
                onAddTier = { _, _, _, _ -> },
                onSetDisplayMode = {},
                signedIn = true,
            )
        }

        composeRule.onNodeWithText("Moves to Street food once it’s approved").assertExists()
    }
}
