package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.PublicationKind
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BoardPublicationController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardPublicationControllerTest {
    @Test
    fun exactlyThirtyPercentDoesNotOffer_andFirstCardAboveItRemembersBeforeShowing() = runTest {
        val h = Harness(this, copy(placed = 6))
        h.controller.loaded(h.visible)
        advanceUntilIdle()
        assertEquals(0, h.community.classified.size)
        assertNull(h.controller.state.value.offer)

        h.replace(copy(placed = 7))
        h.controller.loaded(h.visible)
        advanceUntilIdle()
        val offer = h.controller.state.value.offer!!
        assertEquals(7, offer.placed)
        assertEquals(20, offer.total)
        assertEquals(RankingChoice.Offered, h.repository.board.rankingChoice)
        assertEquals(10L, h.repository.board.originVersion)
        assertTrue(h.community.published.isEmpty())
    }

    @Test
    fun smallListsUseTheSameStrictFraction_withoutMinimumCardCount() = runTest {
        val h = Harness(this, copy(placed = 1, total = 3))
        h.controller.loaded(h.visible)
        advanceUntilIdle()
        assertEquals(1, h.controller.state.value.offer!!.placed)
    }

    @Test
    fun dismissAndReopenDoNotOfferAgain_andProgressIsRetained() = runTest {
        val h = Harness(this)
        val arrangement = h.visible.tiers
        h.controller.loaded(h.visible)
        advanceUntilIdle()
        h.controller.dismissOffer()
        val reopened = Harness(this, h.repository.board)
        reopened.controller.loaded(reopened.visible)
        advanceUntilIdle()
        assertNull(reopened.controller.state.value.offer)
        assertEquals(arrangement, reopened.repository.board.tiers)
        assertTrue(reopened.community.classified.isEmpty())
    }

    @Test
    fun choosingPrivatePreservesPlacementsAndBindings_andNeverPosts() = runTest {
        val h = Harness(this)
        val arrangement = h.visible.tiers
        h.controller.loaded(h.visible)
        advanceUntilIdle()
        h.controller.keepPrivate()
        advanceUntilIdle()
        assertEquals(RankingChoice.Private, h.repository.board.rankingChoice)
        assertEquals(arrangement, h.repository.board.tiers)
        assertNull(h.controller.state.value.offer)
        assertTrue(h.community.published.isEmpty())
    }

    @Test
    fun manuallyChangedCopyDoesNotOfferRanking() = runTest {
        val h = Harness(this)
        h.community.classification = { Result.success(PublicationKind.NewList(BasedOn("source", "Original", "Author"))) }
        h.controller.loaded(h.visible)
        advanceUntilIdle()
        assertNull(h.controller.state.value.offer)
        assertNull(h.repository.board.rankingChoice)
        assertTrue(h.community.published.isEmpty())
    }

    @Test
    fun failedOfferPersistenceDoesNotShowAnUnrememberedDialog() = runTest {
        val h = Harness(this)
        h.repository.failChoice = true
        h.controller.loaded(h.visible)
        advanceUntilIdle()
        assertNull(h.controller.state.value.offer)
        assertNull(h.repository.board.rankingChoice)
        assertTrue(h.community.published.isEmpty())
    }

    @Test
    fun guestPublicChoiceRemembersIntentAndRoutesToSignIn_withoutPosting() = runTest {
        val h = Harness(this, account = Account.Guest)
        val arrangement = h.visible.tiers
        h.controller.loaded(h.visible)
        advanceUntilIdle()
        var signIns = 0
        h.controller.shareOffer { signIns++ }
        advanceUntilIdle()
        assertEquals(1, signIns)
        assertEquals(RankingChoice.Ranking, h.repository.board.rankingChoice)
        assertEquals(arrangement, h.repository.board.tiers)
        assertTrue(h.community.published.isEmpty())

        h.accounts.accounts.value = FakeAccountRepository.signedIn
        h.controller.accountChanged()
        h.controller.loaded(h.visible)
        advanceUntilIdle()
        assertTrue(h.community.published.isEmpty())
        assertNull(h.controller.state.value.offer)
    }

    @Test
    fun signedInPublicOfferPostsOnlyAfterChoice_andUsesSourceVersionAndCategoryFallback() = runTest {
        val h = Harness(this, copy().copy(category = null))
        h.controller.loaded(h.visible)
        advanceUntilIdle()
        assertTrue(h.community.published.isEmpty())
        h.controller.shareOffer { error("Already signed in") }
        advanceUntilIdle()
        assertEquals(10L, h.community.published.single().originVersion)
        assertEquals("other", h.community.published.single().category)
        assertEquals(0, h.categoryRequests)
        assertEquals(RankingChoice.Ranking, h.repository.board.rankingChoice)
    }

    @Test
    fun manualPublishShowsAuthoritativeTypeBeforeAnyWrite() = runTest {
        val h = Harness(this, copy().copy(rankingChoice = RankingChoice.Private))
        h.community.classification = { Result.success(PublicationKind.NewList(BasedOn("source", "Original", "Author"))) }
        h.controller.setPublic(true)
        advanceUntilIdle()
        assertTrue(h.controller.state.value.preview!!.kind is PublicationKind.NewList)
        assertTrue(h.community.published.isEmpty())
        h.controller.confirmPreview()
        advanceUntilIdle()
        assertEquals(1, h.community.published.size)
    }

    @Test
    fun derivativePreviewPinsSourceVersionForLegacyCopy_withoutChangingItsPublicKind() = runTest {
        val h = Harness(this, copy().copy(originVersion = null))
        h.community.classification = { Result.success(PublicationKind.NewList(BasedOn("source", "Original", "Author"), sourceVersion = 42)) }
        h.controller.setPublic(true)
        advanceUntilIdle()
        assertTrue(h.community.published.isEmpty())
        h.controller.confirmPreview()
        advanceUntilIdle()
        assertEquals(42L, h.community.published.single().originVersion)
    }

    @Test
    fun onlyNewListRequestsCategory_afterExplicitPreviewConfirmation() = runTest {
        val h = Harness(this, copy().copy(category = null))
        h.community.classification = { Result.success(PublicationKind.NewList()) }
        h.controller.setPublic(true)
        advanceUntilIdle()
        assertEquals(0, h.categoryRequests)
        h.controller.confirmPreview()
        advanceUntilIdle()
        assertEquals(1, h.categoryRequests)
        assertTrue(h.community.published.isEmpty())
    }

    @Test
    fun changedLocalRevisionCannotUseOldPreview_andRequiresFreshConfirmation() = runTest {
        val h = Harness(this)
        h.controller.setPublic(true)
        advanceUntilIdle()
        h.replace(h.visible.copy(title = "My edited list"))
        h.community.classification = { Result.success(PublicationKind.NewList()) }
        h.controller.confirmPreview()
        advanceUntilIdle()
        assertTrue(h.community.published.isEmpty())
        assertTrue(h.controller.state.value.preview!!.kind is PublicationKind.NewList)
        assertEquals("My edited list", h.controller.state.value.preview!!.list.title)
        h.controller.confirmPreview()
        advanceUntilIdle()
        assertEquals("My edited list", h.community.published.single().title)
    }

    @Test
    fun databaseEditNotYetShownCannotUseOldPreview() = runTest {
        val h = Harness(this)
        h.controller.setPublic(true)
        advanceUntilIdle()
        h.repository.board = h.visible.copy(title = "Arrived from other phone")
        h.controller.confirmPreview()
        advanceUntilIdle()
        assertTrue(h.community.published.isEmpty())
        assertEquals("Arrived from other phone", h.controller.state.value.preview!!.list.title)
    }

    @Test
    fun latePreflightAfterAccountRoundTripCannotCreatePreviewOrPost() = runTest {
        val h = Harness(this)
        val reply = CompletableDeferred<Result<PublicationKind>>()
        h.community.classification = { reply.await() }
        h.controller.setPublic(true)
        runCurrent()
        h.accounts.accounts.value = Account.Guest
        h.controller.accountChanged()
        h.accounts.accounts.value = FakeAccountRepository.signedIn
        h.controller.accountChanged()
        reply.complete(Result.success(PublicationKind.Ranking("source", 10)))
        advanceUntilIdle()
        assertNull(h.controller.state.value.preview)
        assertFalse(h.controller.state.value.busy)
        assertTrue(h.community.published.isEmpty())
    }

    @Test
    fun accountChangeInvalidatesVisiblePreview() = runTest {
        val h = Harness(this)
        h.controller.setPublic(true)
        advanceUntilIdle()
        h.accounts.accounts.value = Account.SignedIn(email = "b@example.test", photoUrl = null, uid = "B")
        h.controller.accountChanged()
        h.controller.confirmPreview()
        advanceUntilIdle()
        assertNull(h.controller.state.value.preview)
        assertTrue(h.community.published.isEmpty())
    }

    @Test
    fun existingPublicUpdateKeepsItsType_withoutNewPreflight() = runTest {
        val h = Harness(this, copy().copy(publishedId = "existing"))
        h.controller.setPublic(true)
        advanceUntilIdle()
        assertEquals("existing", h.community.published.single().publishedId)
        assertTrue(h.community.classified.isEmpty())
    }

    @Test
    fun legacyRankingWithdrawRemembersChoice_evenWithoutTakenFrom() = runTest {
        val h = Harness(this, copy().copy(takenFrom = null, rankingOf = "source"))
        h.controller.loaded(h.visible)
        h.controller.setPublic(false)
        advanceUntilIdle()
        assertEquals(1, h.withdrawals)
        assertEquals(RankingChoice.Ranking, h.repository.board.rankingChoice)
        assertTrue(h.community.classified.isEmpty())
        assertTrue(h.community.published.isEmpty())
    }

    @Test
    fun sourceChangedRefuses_andExplicitRecheckRetainsLayoutUnbindsIndicesAndNeedsNewPreview() = runTest {
        val h = Harness(this, copy().copy(originVersion = 9))
        val layout = h.visible.tiers.map { it.items.map(TierItem::id) }
        h.community.classification = { board ->
            if (board.originVersion == 9L) Result.failure(PublishRefused(PublishError.SourceChanged))
            else Result.success(PublicationKind.Ranking("source", 10))
        }
        h.controller.setPublic(true)
        advanceUntilIdle()
        assertEquals(PublishError.SourceChanged, h.controller.state.value.error)
        assertTrue(h.community.opened.isEmpty())
        assertTrue(h.community.published.isEmpty())

        h.controller.recheckOriginal()
        advanceUntilIdle()
        assertEquals(listOf("source"), h.community.opened)
        assertEquals(10L, h.repository.board.originVersion)
        assertEquals(layout, h.repository.board.tiers.map { it.items.map(TierItem::id) })
        assertTrue(h.repository.board.tiers.flatMap(Tier::items).all { it.sourceIndex == null })
        assertEquals(PublicationKind.Ranking("source", 10), h.controller.state.value.preview!!.kind)
        assertTrue(h.community.published.isEmpty())
        h.controller.confirmPreview()
        advanceUntilIdle()
        assertEquals(10L, h.community.published.single().originVersion)
    }

    @Test
    fun originalRecheckCannotChangeVersionAfterLocalEditOrAccountChange() = runTest {
        val h = Harness(this, copy().copy(originVersion = 9))
        val opened = CompletableDeferred<Result<PublishedList>>()
        h.community.opening = { opened.await() }
        h.controller.recheckOriginal()
        runCurrent()
        h.replace(h.visible.copy(title = "Edited during recheck"))
        opened.complete(Result.success(original()))
        advanceUntilIdle()
        assertEquals(9L, h.repository.board.originVersion)
        assertNull(h.controller.state.value.preview)
        assertTrue(h.community.published.isEmpty())
    }

    @Test
    fun sourceCanChangeAgainAfterPreview_andServerRefusalStaysVisible() = runTest {
        val h = Harness(this)
        h.community.publishResult = Result.failure(PublishRefused(PublishError.SourceChanged))
        h.controller.setPublic(true)
        advanceUntilIdle()
        h.controller.confirmPreview()
        advanceUntilIdle()
        assertEquals(PublishError.SourceChanged, h.controller.state.value.error)
        assertEquals(1, h.community.published.size)
        assertEquals(0, h.recorded)
    }

    private class Harness(scope: CoroutineScope, board: TierList = copy(), account: Account = FakeAccountRepository.signedIn) {
        val repository = Boards(board)
        val community = Community()
        val accounts = FakeAccountRepository(account)
        var visible = board
        var categoryRequests = 0
        var withdrawals = 0
        var recorded = 0
        val controller = BoardPublicationController(
            repository, community, accounts, scope,
            current = { visible }, busy = { _, _ -> }, error = {},
            wantsCategory = { categoryRequests++ },
            takeDown = {
                withdrawals++
                Result.success(null)
            },
            recorded = { _, result ->
                recorded++
                repository.board = repository.board.copy(publishedId = result?.takeUnless { it.ranking }?.id, rankingOf = result?.takeIf { it.ranking }?.id)
            },
            reload = { visible = repository.board },
        )

        fun replace(board: TierList) {
            repository.board = board
            visible = board
        }
    }

    private class Boards(var board: TierList) : TierRepository by RecordingTierRepository() {
        var failChoice = false
        override suspend fun getTierListById(id: Long): TierList = board
        override suspend fun setRankingChoice(id: Long, choice: RankingChoice) {
            check(!failChoice)
            board = board.copy(rankingChoice = choice)
        }
        override suspend fun setRankingOrigin(id: Long, sourceVersion: Long) {
            val changedVersion = board.originVersion != null && board.originVersion != sourceVersion
            board = board.copy(
                originVersion = sourceVersion,
                tiers = board.tiers.map { tier ->
                tier.copy(items = tier.items.map { if (changedVersion) it.copy(sourceIndex = null) else it })
            },
            )
        }
    }

    private class Community : FakeCommunityRepository() {
        val classified = mutableListOf<TierList>()
        val published = mutableListOf<TierList>()
        val opened = mutableListOf<String>()
        var classification: suspend (TierList) -> Result<PublicationKind> = { Result.success(PublicationKind.Ranking("source", 10)) }
        var opening: suspend () -> Result<PublishedList> = { Result.success(original()) }
        var publishResult = Result.success(Published("source", "fp", ranking = true))
        override suspend fun classifyPublication(list: TierList): Result<PublicationKind> {
            classified += list
            return classification(list)
        }
        override suspend fun publish(list: TierList): Result<Published> {
            published += list
            return publishResult
        }
        override suspend fun open(id: String): Result<PublishedList> {
            opened += id
            return opening()
        }
    }

    private companion object {
        fun copy(placed: Int = 7, total: Int = 20): TierList {
            val cards = (0 until total).map { TierItem(id = it.toLong(), title = "Card $it", imageUrl = null, sourceIndex = it) }
            return TierList(
                id = 1, title = "Original", takenFrom = "source", category = "art", uid = "board",
                tiers = listOf(
                    Tier(1, "A", "#000000", "#000000", cards.take(placed)),
                    Tier(2, "Pool", "#000000", "#000000", cards.drop(placed), isPool = true),
                ),
            )
        }
        fun original() = PublishedList(
            PublishedListSummary("source", "Original", "author", "Author", category = "art", itemCount = 20, updatedAtMillis = 10),
            tiers = emptyList(), items = emptyList(),
        )
    }
}
