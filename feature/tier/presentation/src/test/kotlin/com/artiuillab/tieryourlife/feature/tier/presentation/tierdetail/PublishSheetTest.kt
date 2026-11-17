package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.PublishSheet
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.PublishSheetCalls
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.PublishSheetState
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.PublishSheetTags
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The publish sheet (Claude Design, 16.11.2026): section, topic or a proposed one, one explanation, Publish with its ring. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-port-xxhdpi")
class PublishSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val said = mutableListOf<String>()

    private val calls = PublishSheetCalls(
        onSection = { said += "section $it" },
        onTopic = { said += "topic ${it?.name}" },
        onProposeTopic = { said += "propose $it" },
        onProposeSection = { said += "section form $it" },
        onPublish = { said += "publish" },
        onName = { said += "name $it" },
        onSignInAndPublish = { said += "sign in and publish" },
    )

    @Test
    fun anUntitledList_asksForAName_beforeItGoesOut() {
        show(board.copy(title = "Untitled list", category = "film_tv"), PublishSheetState(topics = topics))
        composeRule.onNodeWithText("Publish your list").assertExists()
        composeRule.onNodeWithText("It’s still called “Untitled list”. People find lists by name.").assertExists()
        composeRule.onNodeWithTag(PublishSheetTags.PUBLISH).performClick()
        composeRule.onNodeWithText("Give the list a name first.").assertExists()
        capture("publish_sheet_untitled")
        assertEquals(emptyList<String>(), said)

        composeRule.onNodeWithTag(PublishSheetTags.NAME).performTextInput("Every A24 film")
        composeRule.onNodeWithTag(PublishSheetTags.PUBLISH).performClick()

        assertEquals(listOf("name Every A24 film", "publish"), said)
    }

    @Test
    fun aGuest_isToldAtTheTop_andTheButtonSignsInAndPublishes() {
        show(board.copy(title = "Untitled list", category = "film_tv"), PublishSheetState(topics = topics), signedIn = false)
        composeRule.onNodeWithTag(PublishSheetTags.NEEDS_ACCOUNT)
            .assertTextContains("Publishing needs a Google account. Your list stays here while you sign in.")
        composeRule.onNodeWithTag(PublishSheetTags.PUBLISH).assertTextContains("Sign in and publish")
        composeRule.onNodeWithTag(PublishSheetTags.NAME).performTextInput("Every A24 film")
        capture("publish_sheet_guest")
        composeRule.onNodeWithTag(PublishSheetTags.PUBLISH).performClick()

        assertEquals(listOf("name Every A24 film", "sign in and publish"), said)
    }

    @Test
    fun signedIn_thereIsNoWordAboutAnAccount() {
        show(board.copy(category = "film_tv"), PublishSheetState(topics = topics))
        composeRule.onNodeWithTag(PublishSheetTags.NEEDS_ACCOUNT).assertDoesNotExist()
        composeRule.onNodeWithTag(PublishSheetTags.PUBLISH).assertTextContains("Publish")
    }

    @Test
    fun aNamedList_hasNoNameField() {
        show(board.copy(category = "film_tv"), PublishSheetState(topics = topics))
        composeRule.onNodeWithTag(PublishSheetTags.NAME).assertDoesNotExist()
    }

    @Test
    fun noSectionYet_publishPointsAtIt() {
        show(board, PublishSheetState(sectionWanted = true))
        composeRule.onNodeWithText("Pick a section first.").assertExists()
        composeRule.onNodeWithText("Pick a section to see its topics.").assertExists()
        capture("publish_sheet_section_wanted")
        composeRule.onNodeWithTag(PublishSheetTags.section("film_tv")).performClick()
        assertEquals(listOf("section film_tv"), said)
    }

    @Test
    fun aSection_showsItsTopics_andASecondTapTakesTheChosenOneOff() {
        show(board.copy(category = "film_tv", topicId = "t1", topicName = "A24"), PublishSheetState(topics = topics), dark = true)
        capture("publish_sheet_topics_dark")
        composeRule.onNodeWithTag(PublishSheetTags.topic("A24")).performClick()
        composeRule.onNodeWithTag(PublishSheetTags.topic("Horror")).performClick()
        assertEquals(listOf("topic null", "topic Horror"), said)
    }

    @Test
    fun aProposedTopic_isAdded() {
        show(board.copy(category = "film_tv"), PublishSheetState(topics = topics))
        composeRule.onNodeWithTag(PublishSheetTags.PROPOSE_TOPIC).performClick()
        composeRule.onNodeWithTag(PublishSheetTags.TOPIC_NAME).performTextInput("A24 horror")
        capture("publish_sheet_propose")
        composeRule.onNodeWithTag(PublishSheetTags.ADD_TOPIC).performClick()
        assertEquals(listOf("propose A24 horror"), said)
    }

    @Test
    fun ticked_itIsANewSection_andGoesToTheSectionForm() {
        show(board.copy(category = "film_tv"), PublishSheetState(topics = topics))
        composeRule.onNodeWithTag(PublishSheetTags.PROPOSE_TOPIC).performClick()
        composeRule.onNodeWithTag(PublishSheetTags.TOPIC_NAME).performTextInput("Directors")
        composeRule.onNodeWithTag(PublishSheetTags.AS_SECTION).performClick()
        composeRule.onNodeWithText("Continue").assertExists()
        capture("publish_sheet_propose_section")
        composeRule.onNodeWithTag(PublishSheetTags.ADD_TOPIC).performClick()
        assertEquals(listOf("section form Directors"), said)
    }

    @Test
    fun aProposedTopic_isMarkedNew_andATapTakesItOff() {
        show(board.copy(category = "film_tv", newTopicName = "A24 horror"), PublishSheetState(topics = topics))
        composeRule.onNodeWithTag(PublishSheetTags.topic("A24 horror")).assertTextContains("A24 horror · new").performClick()
        assertEquals(listOf("topic null"), said)
    }

    @Test
    fun theCover_comesFirst_andChangeOpensThePicker() {
        show(board.copy(category = "film_tv"), PublishSheetState(topics = topics), calls = calls.copy(onChangeCover = { said += "cover" }))
        composeRule.onNodeWithText("Tier colours · auto").assertExists()
        capture("publish_sheet_cover")
        composeRule.onNodeWithTag(PublishSheetTags.CHANGE_COVER).assertContentDescriptionEquals("Change the cover").performClick()
        assertEquals(listOf("cover"), said)
    }

    @Test
    fun withNoWayToThePicker_thereIsNoCover() {
        show(board.copy(category = "film_tv"), PublishSheetState(topics = topics))
        assertEquals(0, composeRule.onAllNodesWithTag(PublishSheetTags.COVER).fetchSemanticsNodes().size)
    }

    @Test
    fun whileItGoes_theButtonTurns_andTakesNoSecondTap() {
        show(board.copy(category = "film_tv"), PublishSheetState(topics = topics), busy = true)
        composeRule.onNodeWithText("Publishing…").assertExists()
        composeRule.onNodeWithTag(PublishSheetTags.PUBLISH).performClick()
        capture("publish_sheet_sending")
        assertEquals(emptyList<String>(), said)
    }

    @Test
    fun aRefusal_isSaidAboveTheButton() {
        show(board.copy(category = "film_tv"), PublishSheetState(topics = topics), error = PublishError.TopicUnavailable)
        composeRule.onNodeWithTag(PublishSheetTags.ERROR).performScrollTo().assertExists()
        composeRule.onNodeWithTag(PublishSheetTags.PUBLISH).performClick()
        assertEquals(listOf("publish"), said)
    }

    private fun show(
        list: TierList,
        state: PublishSheetState,
        busy: Boolean = false,
        error: PublishError? = null,
        dark: Boolean = false,
        calls: PublishSheetCalls = this.calls,
        signedIn: Boolean = true,
    ) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = dark) {
                PublishSheet(list = list, state = state, busy = busy, error = error, calls = calls, signedIn = signedIn)
            }
        }
        composeRule.waitForIdle()
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(name: String) {
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
        val topics = listOf("Horror", "A24", "Sci-fi", "Animation", "Comedy").mapIndexed { i, name ->
            Topic(if (name == "A24") "t1" else "x$i", name, "film_tv", TopicStatus.Approved)
        }
        val board = TierList(
            1, "A24 films, ranked",
            listOf(
                Tier(10, "S", "#E8A0A0", "#824848", List(24) { TierItem(it.toLong(), "Card $it", null) }),
                Tier(11, "Unranked", "#DDDDDD", "#444444", emptyList(), isPool = true),
            ),
        )
    }
}
