package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
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
import com.artiuillab.tieryourlife.feature.community.domain.model.ListSource
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowAfterSignIn
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val stream = ListSource("Nix's fruit stream", "https://www.twitch.tv/videos/123456789")

private fun fruit(authorUid: String, anonymous: Boolean) = PublishedList(
    summary = PublishedListSummary(
        id = "h1",
        title = "Fruit, ranked live",
        authorUid = authorUid,
        authorName = if (anonymous) "" else "Olena",
        category = "food",
        itemCount = 1,
        updatedAtMillis = 0,
        anonymous = anonymous,
    ),
    tiers = listOf(Tier(id = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList())),
    items = listOf(TierItem(0, "Mango", null)),
    arrangement = listOf(0),
    source = stream,
)

/** "Source: …" under a house list's title, a tap from the stream it was ranked on; nobody else's list says it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class ListSourceLineTest {

    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<String>()

    private fun showPage(list: PublishedList) {
        val viewModel = CommunityListViewModel(
            community = object : FakeCommunityRepository() {
                override suspend fun open(id: String): Result<PublishedList> = Result.success(list)
            },
            tiers = RecordingTierRepository(),
            preferences = FakeAppPreferences(),
            savedStateHandle = SavedStateHandle(mapOf("publishedId" to list.summary.id)),
            accounts = FakeAccountRepository(),
            openedLists = FakeOpenedLists(),
            afterSignIn = FollowAfterSignIn(FakeAccountRepository()),
        )
        val links = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        compose.setContent {
            TierYourLifeTheme {
                CompositionLocalProvider(LocalUriHandler provides links) {
                    CommunityListScreen(onBack = {}, onOpenSaved = {}, onAuthorClick = { _, _, _ -> }, viewModel = viewModel)
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(CommunityListTestTags.SAVE).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun aHouseList_namesItsSource_andATapOpensIt() {
        showPage(fruit(authorUid = "tyl-editorial", anonymous = true))

        compose.onNodeWithText("Source: Nix's fruit stream").assertExists()
        compose.onNodeWithTag(CommunityListTestTags.SOURCE).performClick()

        assertEquals(listOf(stream.url), opened)
    }

    @Test
    fun somebodysList_saysNoSource_whateverCameWithIt() {
        showPage(fruit(authorUid = "u-olena", anonymous = false))

        compose.onNodeWithTag(CommunityListTestTags.SOURCE).assertDoesNotExist()
    }
}
