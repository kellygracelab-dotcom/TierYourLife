package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.PublicationKind
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
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
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

@RunWith(AndroidJUnit4::class)
class PublicationPreviewUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var vm: TierDetailViewModel
    private val posted = mutableListOf<TierList>()

    @After fun stop() {
        if (::vm.isInitialized) vm.viewModelScope.cancel()
    }

    @Test fun theRankingPreviewCanBeCancelledWithoutAPublicRequestOrLocalPublication() {
        val boards = show(PublicationKind.Ranking("original", 123))
        askToPublish()
        val text = InstrumentationRegistry.getInstrumentation().targetContext.getString(ThemeR.string.publication_preview_ranking)
        compose.onNodeWithText(text, substring = true).assertIsDisplayed()
        compose.onNodeWithTag("publication_cancel").performClick()
        compose.onNodeWithTag("publication_preview").assertDoesNotExist()
        compose.runOnIdle {
            assertTrue(posted.isEmpty())
            assertTrue(boards.publishedIds.isEmpty())
            assertTrue(boards.rankings.isEmpty())
        }
    }

    @Test fun theAttributedListPreviewRequiresConfirmAndPublishesThePinnedSourceSnapshotOnce() {
        val boards = show(PublicationKind.NewList(BasedOn("original", "Original", "Author"), sourceVersion = 123))
        askToPublish()
        val text = InstrumentationRegistry.getInstrumentation().targetContext.getString(ThemeR.string.publication_preview_list)
        compose.onNodeWithText(text, substring = true).assertIsDisplayed()
        compose.runOnIdle { assertTrue(posted.isEmpty()) }
        compose.onNodeWithTag("publication_confirm").performClick()
        compose.waitUntil(5_000) { posted.size == 1 && !vm.publicationState.value.busy }
        compose.runOnIdle {
            assertEquals(123L, posted.single().originVersion)
            assertEquals(listOf(1L to "independent"), boards.publishedIds)
            assertEquals(listOf(1L to null), boards.rankings)
        }
    }

    private fun show(kind: PublicationKind): RecordingTierRepository {
        val board = TierList(
            1, "My copy",
                listOf(
                Tier(10, "S", "#E8A0A0", "#824848", List(3) { TierItem(it.toLong(), "Card $it", null, sourceIndex = it) }),
                Tier(11, "Unranked", "#DDDDDD", "#444444", emptyList(), isPool = true),
            ),
            category = "other", uid = "my-copy", takenFrom = "original", originVersion = 123, rankingChoice = RankingChoice.Private,
        )
        val boards = RecordingTierRepository(boards = listOf(board))
        val accounts = FakeAccountRepository().apply { accounts.value = Account.SignedIn("demo@example.com", null, uid = "demo") }
        val service = object : FakeCommunityRepository() {
            override suspend fun classifyPublication(list: TierList) = Result.success(kind)
            override suspend fun publish(list: TierList): Result<Published> {
                posted += list
                return Result.success(Published("independent", "snapshot"))
            }
        }
        compose.runOnUiThread { vm = TierDetailViewModel(boards, service, accounts, SavedStateHandle(mapOf("tierListId" to 1L))) }
        compose.setContent { TierYourLifeTheme { TierDetailScreen(onBack = {}, viewModel = vm) } }
        compose.waitUntil(5_000) { vm.screen.value.board is TierDetailUiState.Success }
        return boards
    }

    private fun askToPublish() {
        compose.runOnIdle { vm.setPublic(true) }
        compose.onNodeWithTag("publication_preview").assertIsDisplayed()
    }
}
