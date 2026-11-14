package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeOpenedLists
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowAfterSignIn
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RankingChoiceUiJourneyTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var vm: CommunityListViewModel
    private var publicCalls = 0

    @After fun stop() {
        if (::vm.isInitialized) vm.viewModelScope.cancel()
    }

    @Test fun notNowClosesTheOfferWithoutSavingOrPublishing() {
        val boards = show(signedIn = true)
        placeAll()
        compose.onNodeWithTag("ranking_choice_dialog").assertIsDisplayed()
        compose.onNodeWithTag("ranking_choice_not_now").performClick()
        compose.onNodeWithTag("ranking_choice_dialog").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(null, (vm.state.value as CommunityListUiState.Success).savedId)
            assertTrue(boards.templates.isEmpty())
            assertTrue(boards.publishedIds.isEmpty())
            assertTrue(boards.rankings.isEmpty())
            assertEquals(0, publicCalls)
        }
    }

    @Test fun guestPublicButtonKeepsPrivateProgressAndUsesSignInWithoutAnAutomaticPublicAction() {
        var signIns = 0
        val boards = show(signedIn = false, onSignIn = { signIns++ })
        placeAll()
        compose.onNodeWithTag("ranking_choice_dialog").assertIsDisplayed()
        compose.onNodeWithTag("ranking_choice_public").performClick()
        compose.waitUntil(5_000) { signIns == 1 }
        compose.runOnIdle {
            assertEquals(20, boards.templates.last().tiers.sumOf { it.items.size })
            assertTrue(boards.publishedIds.isEmpty())
            assertTrue(boards.rankings.isEmpty())
            assertEquals(0, publicCalls)
        }
    }

    private fun show(signedIn: Boolean, onSignIn: () -> Unit = {}): RecordingTierRepository {
        val boards = RecordingTierRepository()
        val accounts = FakeAccountRepository().apply { if (signedIn) accounts.value = Account.SignedIn("demo@example.com", null, uid = "demo") }
        val service = object : FakeCommunityRepository() {
            override suspend fun publish(list: com.artiuillab.tieryourlife.feature.tier.domain.model.TierList) =
                super.publish(list).also { publicCalls++ }

            override suspend fun showMyRankingVersioned(listId: String, arrangement: List<Int?>, rows: List<List<Int>>, tierLabels: List<String>, version: Long?, expectedOwnerUid: String?, board: TierList?) =
                super.showMyRankingVersioned(listId, arrangement, rows, tierLabels, version, expectedOwnerUid, board).also { publicCalls++ }

            override suspend fun open(id: String) = Result.success(
                PublishedList(
                    PublishedListSummary(id, "Twenty cards", "author", "Author", category = "other", itemCount = 20, updatedAtMillis = 123),
                    listOf(Tier(0, "S", "#E8A0A0", "#824848", emptyList())),
                    List(20) { TierItem(it.toLong(), "Card $it", null, sourceIndex = it) },
                ),
            )
        }
        val records = mutableMapOf<Pair<String?, String>, RankingDraft>()
        val drafts = object : RankingDrafts {
            override suspend fun read(listId: String, owner: String?) = records[owner to listId]
            override suspend fun keep(listId: String, draft: RankingDraft, owner: String?) {
                records[owner to listId] = draft
            }
        }
        compose.runOnUiThread {
            vm = CommunityListViewModel(
                service, boards, FakeAppPreferences(), SavedStateHandle(mapOf("publishedId" to "native-source")),
                accounts, FakeOpenedLists(), FollowAfterSignIn(accounts), drafts,
            )
        }
        compose.setContent {
            TierYourLifeTheme { CommunityListScreen(onBack = {}, onOpenSaved = {}, onAuthorClick = { _, _, _ -> }, onSignIn = onSignIn, viewModel = vm) }
        }
        compose.waitUntil(5_000) { vm.state.value is CommunityListUiState.Success }
        return boards
    }

    private fun placeAll() {
        compose.runOnIdle { repeat(20) { vm.moveItem(it.toLong(), 0, it) } }
    }
}
