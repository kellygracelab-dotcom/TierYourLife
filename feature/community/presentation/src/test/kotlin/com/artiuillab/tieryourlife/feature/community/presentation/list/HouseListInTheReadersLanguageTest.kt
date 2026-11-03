package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.model.ListTranslations
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedCard
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val ghibliSummary = PublishedListSummary(
    id = "NI9W1YK6aNpeDfz4DQQb",
    title = "Studio Ghibli Movies",
    authorUid = "tyl-editorial",
    authorName = "",
    category = "anime",
    itemCount = 2,
    updatedAtMillis = 0,
    anonymous = true,
    names = mapOf("ru" to "Фильмы студии Ghibli"),
)

private val ghibli = PublishedList(
    summary = ghibliSummary,
    tiers = listOf(
        Tier(id = 0, label = "Masterpiece", colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList()),
        Tier(id = 1, label = "Great", colorLight = "#C06A25", colorDark = "#E9A867", items = emptyList()),
    ),
    items = listOf(TierItem(0, "Spirited Away", null), TierItem(1, "My Neighbor Totoro", null)),
    arrangement = listOf(0, 1),
    translations = ListTranslations(
        mapOf(
            "ru" to mapOf(
                "Studio Ghibli Movies" to "Фильмы студии Ghibli",
                "Masterpiece" to "Шедевр",
                "Spirited Away" to "Унесённые призраками",
            ),
        ),
    ),
)

/** A house list reads in the app's language on its card, on its page, and in the copy a reader keeps. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "ru-w411dp-h891dp")
class HouseListInTheReadersLanguageTest {

    @get:Rule
    val compose = createComposeRule()

    private val tiers = RecordingTierRepository()

    private fun showCard() = compose.setContent {
        TierYourLifeTheme { FeedCard(summary = ghibliSummary, onClick = {}, onLongClick = {}) }
    }

    private fun showPage() {
        val viewModel = CommunityListViewModel(
            community = object : FakeCommunityRepository() {
                override suspend fun open(id: String): Result<PublishedList> = Result.success(ghibli)
            },
            tiers = tiers,
            preferences = FakeAppPreferences(),
            savedStateHandle = SavedStateHandle(mapOf("publishedId" to ghibli.summary.id)),
            accounts = FakeAccountRepository(),
            openedLists = FakeOpenedLists(),
        )
        compose.setContent {
            TierYourLifeTheme { CommunityListScreen(onBack = {}, onOpenSaved = {}, onAuthorClick = { _, _, _ -> }, viewModel = viewModel) }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(CommunityListTestTags.SAVE).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun saveTheirs() {
        compose.onNodeWithTag(CommunityListTestTags.SAVE).performClick()
        compose.onNodeWithTag(CommunityListTestTags.SAVE_THEIRS).performClick()
        compose.waitUntil(5_000) { tiers.templates.isNotEmpty() }
    }

    @Test
    fun theCard_namesTheListInRussian() {
        showCard()

        compose.onNodeWithText("Фильмы студии Ghibli").assertExists()
    }

    @Test
    @Config(qualifiers = "pt-rPT-w411dp-h891dp")
    fun europeanPortuguese_readsTheTitleAsWritten() {
        showCard()

        compose.onNodeWithText("Studio Ghibli Movies").assertExists()
    }

    @Test
    fun thePage_drawsTitleTiersAndCardsInRussian_andWhatIsNotTranslatedAsWritten() {
        showPage()

        compose.onNodeWithText("Фильмы студии Ghibli").assertExists()
        compose.onNodeWithText("Шедевр", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Унесённые призраками", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("My Neighbor Totoro", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theCopy_keepsTheWordsItWasReadIn() {
        showPage()

        saveTheirs()

        val copy = tiers.templates.single()
        assertEquals("Фильмы студии Ghibli", copy.title)
        assertEquals(listOf("Шедевр", "Great"), copy.tiers.map { it.label })
        assertEquals(listOf(listOf("Унесённые призраками"), listOf("My Neighbor Totoro")), copy.tiers.map { tier -> tier.items.map { it.title } })
        assertEquals(ghibli.summary.id, copy.takenFrom)
    }

    @Test
    @Config(qualifiers = "en-w411dp-h891dp")
    fun inEnglish_theCopyIsTheListAsWritten() {
        showPage()

        saveTheirs()

        val copy = tiers.templates.single()
        assertEquals("Studio Ghibli Movies", copy.title)
        assertEquals(listOf("Masterpiece", "Great"), copy.tiers.map { it.label })
        assertEquals(listOf(listOf("Spirited Away"), listOf("My Neighbor Totoro")), copy.tiers.map { tier -> tier.items.map { it.title } })
    }
}
