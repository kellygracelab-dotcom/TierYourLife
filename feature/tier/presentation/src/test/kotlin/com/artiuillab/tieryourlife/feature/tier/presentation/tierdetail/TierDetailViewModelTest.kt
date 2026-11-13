package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublicationKind
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.summary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Instrumented because viewModelScope needs Android's main dispatcher.
class TierDetailViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun addManualItem_withTitleOnly_addsToThePoolWithNoImage() = runBlocking {
        val repository = FakeTierRepository(pool())
        val viewModel = TierDetailViewModel(repository, FakeCommunityRepositoryForDetail(), FakeAccountRepository(), savedStateHandle())

        viewModel.addManualItem("Dune", photoUris = emptyList())
        val state = viewModel.board.firstWithin {
            it is TierDetailUiState.Success && it.list.tiers.single().items.isNotEmpty()
        } as TierDetailUiState.Success

        val added = state.list.tiers.single().items.single()
        assertEquals("Dune", added.title)
        assertNull(added.imageUrl)
        assertEquals(emptyList<String>(), repository.attachedSources)
    }

    @Test
    fun addManualItem_withAPhoto_sendsTheGalleryUriOnlyToAttachImageToItem() = runBlocking {
        val repository = FakeTierRepository(pool())
        val viewModel = TierDetailViewModel(repository, FakeCommunityRepositoryForDetail(), FakeAccountRepository(), savedStateHandle())

        viewModel.addManualItem("Dune", photoUris = listOf("content://gallery/42"))
        viewModel.board.firstWithin {
            it is TierDetailUiState.Success && it.list.tiers.single().items.isNotEmpty()
        }

        assertEquals(listOf("content://gallery/42"), repository.attachedSources)
        assertNull(repository.lastAddedImageUrl)
    }

    @Test
    fun addManualItem_withSeveralPhotos_addsOneItemPerPhoto() = runBlocking {
        val repository = FakeTierRepository(pool())
        val viewModel = TierDetailViewModel(repository, FakeCommunityRepositoryForDetail(), FakeAccountRepository(), savedStateHandle())
        val picked = listOf("content://gallery/1", "content://gallery/2", "content://gallery/3")

        viewModel.addManualItem("", photoUris = picked)
        val state = viewModel.board.firstWithin {
            it is TierDetailUiState.Success && it.list.tiers.single().items.size == picked.size
        } as TierDetailUiState.Success

        assertEquals(picked, repository.attachedSources)
        assertEquals(listOf("", "", ""), state.list.tiers.single().items.map { it.title })
        assertNull(repository.lastAddedImageUrl)
    }

    @Test
    fun reorderTiers_emitsTheNewOrderSynchronously_beforeTheRepositoryResponds() = runBlocking {
        val repository = FakeTierRepository(rankedList())
        val viewModel = TierDetailViewModel(repository, FakeCommunityRepositoryForDetail(), FakeAccountRepository(), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.reorderTiers(listOf(20L, 10L))

        val optimistic = viewModel.screen.value.board as TierDetailUiState.Success
        assertEquals(listOf(20L, 10L, 30L), optimistic.list.tiers.map { it.id })

        repository.reorderStarted.await()
        assertEquals(listOf(20L, 10L), repository.lastReorderedIds)
        val stillOptimistic = viewModel.screen.value.board as TierDetailUiState.Success
        assertEquals(listOf(20L, 10L, 30L), stillOptimistic.list.tiers.map { it.id })

        repository.releaseReorder()
    }

    @Test
    fun setPublic_publishesTheListAndRemembersWhereItLanded() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard())
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            repository,
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals(1, community.published.size)
        assertEquals("published-1", repository.publishedIds.single().second)
    }

    @Test
    fun newPublicationWaitsForExplicitServerPreviewConfirmation() = runBlocking {
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(FakeTierRepository(listWithOneCard()), community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.setPublic(true)
        val preview = viewModel.publicationState.firstWithin { it.preview != null && !it.busy }.preview!!
        assertEquals(PublicationKind.NewList(), preview.kind)
        assertTrue(community.published.isEmpty())
        viewModel.confirmPublication()
        community.sent.firstWithin { it.isNotEmpty() }
        assertEquals(1, community.published.size)
    }

    @Test
    fun crossingThirtyPercentOffersOnce_andPrivateChoicePreservesTheMovedCard() = runBlocking {
        val cards = (1L..20L).map { TierItem(it, "Card $it", null, sourceIndex = (it - 1).toInt()) }
        val sourceCopy = TierList(
            1, "Original", takenFrom = "src-1",
            tiers = listOf(
            Tier(10, "A", "#000000", "#000000", cards.take(6)),
            Tier(20, "Pool", "#000000", "#000000", cards.drop(6), isPool = true),
        ),
        )
        val repository = FakeTierRepository(sourceCopy)
        val community = FakeCommunityRepositoryForDetail(publicationKind = PublicationKind.Ranking("src-1", 10))
        val accounts = FakeAccountRepository(FakeAccountRepository.signedIn)
        val viewModel = TierDetailViewModel(repository, community, accounts, savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        assertNull(viewModel.publicationState.value.offer)

        repository.releaseMoves()
        viewModel.moveItem(7, 10, 6)
        viewModel.publicationState.firstWithin { it.offer != null }
        assertEquals(RankingChoice.Offered, repository.getTierListById(1)!!.rankingChoice)
        viewModel.keepRankingPrivate()
        viewModel.publicationState.firstWithin { it.offer == null && !it.busy }
        val saved = repository.getTierListById(1)!!
        assertEquals(RankingChoice.Private, saved.rankingChoice)
        assertEquals((1L..7L).toList(), saved.tiers.first().items.map(TierItem::id))
        assertTrue(community.published.isEmpty())

        val reopened = TierDetailViewModel(repository, community, accounts, savedStateHandle())
        reopened.board.firstWithin { it is TierDetailUiState.Success }
        assertNull(reopened.publicationState.value.offer)
    }

    // The author named a topic the server had not seen; publishing is when it becomes
    // one, and the name stays beside the id while a moderator has yet to decide.
    @Test
    fun setPublic_keepsTheWaitingMark_whileTheServerSaysTheTopicWaits() = runBlocking {
        val (repository, viewModel) = publishedWithNewTopic(TopicStatus.Pending)

        assertEquals(listOf(listOf(1L, "t1", "Oscars 2025", "Oscars 2025")), repository.topicsSet)
        val shown = (viewModel.screen.value.board as TierDetailUiState.Success).list
        assertEquals("t1", shown.topicId)
        assertEquals("Oscars 2025", shown.newTopicName)
    }

    @Test
    fun setPublic_dropsTheWaitingMark_whenTheNameWasATopicAlready() = runBlocking {
        val (repository, viewModel) = publishedWithNewTopic(TopicStatus.Approved)

        assertEquals(listOf(listOf(1L, "t1", "Oscars 2025", null)), repository.topicsSet)
        assertNull((viewModel.screen.value.board as TierDetailUiState.Success).list.newTopicName)
    }

    // Merged away, turned down or somebody else's: the server will not take it, so the board stops asking.
    @Test
    fun setPublic_forgetsATopicTheServerWillNotTake() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard().copy(topicId = "t9", topicName = "Old word"))
        val community = FakeCommunityRepositoryForDetail(publishResult = Result.failure(PublishRefused(PublishError.TopicUnavailable)))
        val viewModel = TierDetailViewModel(repository, community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.publishAfterPreview()
        viewModel.board.firstWithin { (it as? TierDetailUiState.Success)?.list?.topicId == null }

        assertEquals(PublishError.TopicUnavailable, viewModel.screen.value.publish.error)
        assertEquals(listOf(listOf(1L, null, null, null)), repository.topicsSet)
    }

    // The same cards as the list they came from: the server keeps the copy as a ranking of it, and the board remembers that.
    @Test
    fun setPublic_answeredAsARanking_marksTheBoardAsOne_andSaysSo() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard().copy(takenFrom = "src-1"))
        val community = FakeCommunityRepositoryForDetail(publishResult = Result.success(Published("src-1", "sent", ranking = true)))
        val viewModel = TierDetailViewModel(repository, community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        val said = async { viewModel.userMessages.first() }

        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals(listOf(1L to "src-1"), repository.rankingsSet)
        assertEquals(1L to null, repository.publishedIds.last())
        val shown = (viewModel.screen.value.board as TierDetailUiState.Success).list
        assertEquals("src-1", shown.rankingOf)
        assertNull(shown.publishedId)
        assertEquals(UserMessage.PublishedAsRanking, said.await())
    }

    @Test
    fun setPublic_heldForAModerator_saysItWaits() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard())
        val community = FakeCommunityRepositoryForDetail(publishResult = Result.success(Published("p-1", "sent", underReview = true)))
        val viewModel = TierDetailViewModel(repository, community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        val said = async { viewModel.userMessages.first() }

        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals("p-1", (viewModel.screen.value.board as TierDetailUiState.Success).list.publishedId)
        assertEquals(UserMessage.PublishedUnderReview, said.await())
    }

    @Test
    fun setPublic_withPicturesFromOtherSitesLeftOut_saysHowMany() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard())
        val community = FakeCommunityRepositoryForDetail(publishResult = Result.success(Published("p-1", "sent", droppedPictures = 3)))
        val viewModel = TierDetailViewModel(repository, community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        val said = async { viewModel.userMessages.first() }

        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals(UserMessage.PicturesDropped(3), said.await())
    }

    @Test
    fun setPublic_withNoPicturesLeftOut_saysNothing() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard())
        val community = FakeCommunityRepositoryForDetail(publishResult = Result.success(Published("p-1", "sent")))
        val viewModel = TierDetailViewModel(repository, community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertNull(withTimeoutOrNull(300) { viewModel.userMessages.first() })
    }

    @Test
    fun setPublic_heldRanking_saysItWaitsRatherThanOutAsARanking() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard().copy(takenFrom = "src-1"))
        val community = FakeCommunityRepositoryForDetail(publishResult = Result.success(Published("src-1", "sent", ranking = true, underReview = true)))
        val viewModel = TierDetailViewModel(repository, community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        val said = async { viewModel.userMessages.first() }

        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals(UserMessage.PublishedUnderReview, said.await())
    }

    @Test
    fun setPublic_off_takesARankingDownFromTheListItAnswers() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard().copy(takenFrom = "src-1", rankingOf = "src-1"))
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(repository, community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.setPublic(false)
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals(listOf("src-1"), community.withdrawn)
        assertTrue(community.unpublished.isEmpty())
        assertEquals(listOf(1L to null), repository.rankingsSet)
        assertNull((viewModel.screen.value.board as TierDetailUiState.Success).list.rankingOf)
    }

    /** A new publication is sent only after the server's preview has been explicitly accepted. */
    private suspend fun TierDetailViewModel.publishAfterPreview() {
        setPublic(true)
        publicationState.firstWithin { !it.busy }
        if (publicationState.value.preview != null) {
            confirmPublication()
            publicationState.firstWithin { !it.busy }
        }
    }

    /** A board whose new topic name the server took, answering with an id and, when asked about it, with [status]. */
    private suspend fun publishedWithNewTopic(status: TopicStatus): Pair<FakeTierRepository, TierDetailViewModel> {
        val repository = FakeTierRepository(listWithOneCard().copy(newTopicName = "Oscars 2025"))
        val community = FakeCommunityRepositoryForDetail(
            publishResult = Result.success(Published("published-1", "sent", topicId = "t1")),
            topicStatus = status,
        )
        val viewModel = TierDetailViewModel(repository, community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }
        return repository to viewModel
    }

    // The switch reads publicPending while the request is in flight and the
    // list itself afterwards. Reading the list back from the database landed
    // after publicPending was cleared, so for a frame or two the switch sprang
    // back to where it started and the note under it came and went, taking
    // every row below with it.
    @Test
    fun setPublic_marksTheListPublished_beforeTheSwitchStopsPending() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard())
        val viewModel = TierDetailViewModel(
            repository,
            FakeCommunityRepositoryForDetail(),
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        repository.holdReadsAfterPublish = true

        viewModel.publishAfterPreview()
        // Waiting on the pending flag itself rather than on `publishing`: those
        // are two writes one after the other, and waking between them made this
        // fail on a slow emulator for a reason that had nothing to do with the
        // thing being tested.
        viewModel.screen.firstWithin { it.publish.publicPending == null }

        val shown = (viewModel.screen.value.board as TierDetailUiState.Success).list
        assertEquals("published-1", shown.publishedId)
    }

    @Test
    fun setPublic_neverSendsTheScreenBackThroughLoading() = runBlocking {
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard()),
            FakeCommunityRepositoryForDetail(),
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        val seen = mutableListOf<TierDetailUiState>()
        val watcher = launch(Dispatchers.Unconfined) { viewModel.board.collect { seen += it } }

        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }
        viewModel.setPublic(false)
        viewModel.screen.firstWithin { !it.publish.publishing }
        watcher.cancel()

        assertTrue(seen.none { it is TierDetailUiState.Loading })
    }

    // Publishing without a category would land the list in a feed nobody
    // browses. The screen asks for one rather than refusing: the category is a
    // row away, so the tap collects what is missing instead of complaining.
    @Test
    fun setPublic_withoutACategory_asksForOneRatherThanPublishing() = runBlocking {
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard().copy(category = null)),
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.publishAfterPreview()

        assertTrue(viewModel.screen.value.publish.categoryWanted)
        assertNull(viewModel.screen.value.publish.error)
        assertEquals(0, community.published.size)
    }

    @Test
    fun backingOutOfTheCategorySheet_leavesTheListAlone() = runBlocking {
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard().copy(category = null)),
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        viewModel.publishAfterPreview()

        viewModel.categoryNotChosen()

        assertFalse(viewModel.screen.value.publish.categoryWanted)
        assertEquals(0, community.published.size)
    }

    @Test
    fun setCategory_isRememberedAndUnblocksPublishing() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard().copy(category = null))
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            repository,
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.setCategory(ListCategory.Food.id)
        viewModel.board.firstWithin {
            (it as? TierDetailUiState.Success)?.list?.category == ListCategory.Food.id
        }
        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals(ListCategory.Food.id, community.published.single().category)
    }

    @Test
    fun goingAnonymous_onAListAlreadyOut_sendsItAgainAtOnce_withoutTheName() = runBlocking {
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard().copy(category = ListCategory.Food.id), publishedId = "published-1"),
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.setAnonymous(true)
        viewModel.board.firstWithin { (it as? TierDetailUiState.Success)?.list?.anonymous == true }
        community.sent.firstWithin { it.isNotEmpty() }

        assertTrue(community.published.single().anonymous)
    }

    @Test
    fun goingAnonymous_beforePublishing_onlyRemembersItForThePublish() = runBlocking {
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard().copy(category = ListCategory.Food.id)),
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.setAnonymous(true)
        viewModel.board.firstWithin { (it as? TierDetailUiState.Success)?.list?.anonymous == true }
        assertTrue(community.published.isEmpty())

        viewModel.publishAfterPreview()
        community.sent.firstWithin { it.isNotEmpty() }
        assertTrue(community.published.single().anonymous)
    }

    @Test
    fun aSectionOfSomebodysOwn_isKeptAndPublishedByItsId() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard().copy(category = null))
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            repository,
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.setCategory("street-food")
        viewModel.board.firstWithin { (it as? TierDetailUiState.Success)?.list?.category == "street-food" }
        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals("street-food", community.published.single().category)
    }

    // A category edit changes the snapshot: the new server preview still needs its own confirmation.
    @Test
    fun choosingTheCategoryItAskedFor_requiresFreshPreviewConfirmation() = runBlocking {
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard().copy(category = null)),
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        viewModel.publishAfterPreview()

        viewModel.setCategory(ListCategory.Food.id)
        viewModel.publicationState.firstWithin { it.preview != null }
        assertTrue(community.published.isEmpty())
        viewModel.confirmPublication()
        // Waiting for the published id rather than for the pending flag to
        // clear: the flag has not been raised yet when setCategory returns.
        viewModel.board.firstWithin { (it as? TierDetailUiState.Success)?.list?.publishedId != null }

        assertEquals(ListCategory.Food.id, community.published.single().category)
        assertFalse(viewModel.screen.value.publish.categoryWanted)
    }

    // Choosing one from the row, with no switch waiting on it, must not
    // publish anything.
    @Test
    fun choosingACategoryOnItsOwn_publishesNothing() = runBlocking {
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard().copy(category = null)),
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.setCategory(ListCategory.Food.id)
        viewModel.board.firstWithin { (it as? TierDetailUiState.Success)?.list?.category == ListCategory.Food.id }

        assertEquals(0, community.published.size)
    }

    // A refusal from the server is not something an unrelated edit answers, so
    // it has to survive the reload that follows one.
    @Test
    fun aServerRefusal_survivesTheNextEdit() = runBlocking {
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard()),
            FakeCommunityRepositoryForDetail(
                publishResult = Result.failure(PublishRefused(PublishError.TooManyLists)),
            ),
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }

        viewModel.setCategory(ListCategory.Food.id)
        viewModel.board.firstWithin { (it as? TierDetailUiState.Success)?.list?.category == ListCategory.Food.id }

        assertEquals(PublishError.TooManyLists, viewModel.screen.value.publish.error)
    }

    // The server refuses a list with nothing in it. Finding that out over the
    // network spends a round trip to end in a switch that springs back with no
    // explanation, so the screen answers first.
    @Test
    fun setPublic_onAnEmptyList_explainsRatherThanAskingTheServer() = runBlocking {
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            FakeTierRepository(pool()),
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.publishAfterPreview()

        assertEquals(PublishError.NothingToPublish, viewModel.screen.value.publish.error)
        assertEquals(0, community.published.size)
    }

    @Test
    fun setPublic_whenTheServerRefuses_keepsTheReasonOnScreen() = runBlocking {
        val community = FakeCommunityRepositoryForDetail(
            publishResult = Result.failure(PublishRefused(PublishError.TooManyLists)),
        )
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard()),
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.publishAfterPreview()
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals(PublishError.TooManyLists, viewModel.screen.value.publish.error)
    }

    // Turning it off has to reach the server as well: a list that vanishes only
    // from the phone would stay in the feed for everyone else.
    @Test
    fun setPublic_false_takesTheSnapshotDownAndForgetsItsId() = runBlocking {
        val repository = FakeTierRepository(listWithOneCard(), publishedId = "published-1")
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            repository,
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.setPublic(false)
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals(listOf("published-1"), community.unpublished)
        assertEquals(null, repository.publishedIds.single().second)
    }

    // Nothing goes to the trash any more: the card is simply gone from the board.
    @Test
    fun deleteItem_takesTheCardOffTheBoard() = runBlocking {
        val repository = FakeTierRepository(rankedListWithCards())
        val viewModel = TierDetailViewModel(repository, FakeCommunityRepositoryForDetail(), FakeAccountRepository(), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.deleteItem(101)
        val state = viewModel.board.firstWithin { board -> board.itemIds(10L) == listOf(100L, 102L) } as TierDetailUiState.Success

        assertTrue(state.list.tiers.none { tier -> tier.items.any { it.id == 101L } })
    }

    @Test
    fun undo_putsTheCardBackInItsTierAtItsPlace() = runBlocking {
        val repository = FakeTierRepository(rankedListWithCards())
        val viewModel = TierDetailViewModel(repository, FakeCommunityRepositoryForDetail(), FakeAccountRepository(), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.deleteItem(101)
        viewModel.board.firstWithin { board -> board.itemIds(10L) == listOf(100L, 102L) }
        viewModel.restoreItem(101)
        viewModel.board.firstWithin { board -> board.itemIds(10L) == listOf(100L, 101L, 102L) }

        val restored = repository.restoredItems.single()
        assertEquals(10L, restored.tierId)
        assertEquals(1, restored.position)
        assertEquals("Moon", restored.title)
        assertEquals("/pictures/moon.jpg", restored.imageUrl)
        assertEquals(TierItemSource.GENERATED, restored.source)
        assertEquals("uid-moon", restored.uid)
        assertFalse(repository.forgotten.isCompleted)
    }

    @Test
    fun anUndoLetGo_givesUpTheCardsPictureAndCannotBringItBack() = runBlocking {
        val repository = FakeTierRepository(rankedListWithCards())
        val viewModel = TierDetailViewModel(repository, FakeCommunityRepositoryForDetail(), FakeAccountRepository(), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        viewModel.deleteItem(101)
        viewModel.forgetDeletedItem(101)
        viewModel.restoreItem(101)
        viewModel.board.firstWithin { board -> board.itemIds(10L) == listOf(100L, 102L) }
        val forgotten = withTimeout(5_000) { repository.forgotten.await() }

        assertEquals(101L, forgotten.id)
        assertTrue(repository.restoredItems.isEmpty())
    }

    @Test
    fun movesInQuickSuccession_keepEveryCardWhereItWasDropped_andReadTheBoardBackOnce() = runBlocking {
        val repository = FakeTierRepository(rankedListWithCards())
        val viewModel = TierDetailViewModel(repository, FakeCommunityRepositoryForDetail(), FakeAccountRepository(), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        val readsAtOpen = repository.boardReads.value

        withContext(Dispatchers.Main) {
            viewModel.moveItem(100, toTierId = 30, toPosition = 0)
            viewModel.moveItem(101, toTierId = 30, toPosition = 1)
        }
        repository.releaseMoves()
        repository.boardReads.firstWithin { it > readsAtOpen }
        repeat(3) { withContext(Dispatchers.Main) { yield() } }

        assertEquals(listOf(100L, 101L), viewModel.screen.value.board.itemIds(30L))
        assertEquals(listOf(102L), viewModel.screen.value.board.itemIds(10L))
        assertEquals(readsAtOpen + 1, repository.boardReads.value)
    }

    @Test
    fun opening_aBoard_readsOnlyThatBoard_andTheLibraryOnlyWhenTheColumnBesideItShows() = runBlocking {
        val repository = FakeTierRepository(rankedListWithCards())
        val viewModel = TierDetailViewModel(repository, FakeCommunityRepositoryForDetail(), FakeAccountRepository(), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        viewModel.loadTierList()
        viewModel.board.firstWithin { it is TierDetailUiState.Success }

        assertEquals(0, repository.summaryReads)

        viewModel.wantBoardsBeside()
        viewModel.wantBoardsBeside()
        val beside = viewModel.screen.firstWithin { it.allLists.isNotEmpty() }.allLists

        assertEquals(listOf(rankedListWithCards().summary()), beside)
        assertEquals(1, repository.summaryReads)
    }

    private fun TierDetailUiState.itemIds(tierId: Long): List<Long>? =
        (this as? TierDetailUiState.Success)?.list?.tiers?.firstOrNull { it.id == tierId }?.items?.map { it.id }

    private fun rankedListWithCards(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
        tiers = listOf(
            Tier(
                id = 10,
                label = "S",
                colorLight = "#000000",
                colorDark = "#000000",
                items = listOf(
                    TierItem(id = 100, title = "Arrival", imageUrl = null),
                    TierItem(id = 101, title = "Moon", imageUrl = "/pictures/moon.jpg", source = TierItemSource.GENERATED, uid = "uid-moon"),
                    TierItem(id = 102, title = "Alien", imageUrl = null),
                ),
            ),
            Tier(id = 30, label = "Pool", colorLight = "#000000", colorDark = "#000000", items = emptyList(), isPool = true),
        ),
    )

    private fun savedStateHandle(startInTitleEdit: Boolean = false): SavedStateHandle =
        SavedStateHandle(mapOf("tierListId" to 1L, "startInTitleEdit" to startInTitleEdit))

    private fun pool(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
        tiers = listOf(
            Tier(id = 10, label = "Pool", colorLight = "#000000", colorDark = "#000000", items = emptyList(), isPool = true),
        ),
    )

    private fun listWithOneCard(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
        category = ListCategory.FilmTv.id,
        tiers = listOf(
            Tier(
                id = 10,
                label = "Pool",
                colorLight = "#000000",
                colorDark = "#000000",
                items = listOf(TierItem(id = 100, title = "Arrival", imageUrl = null)),
                isPool = true,
            ),
        ),
    )

    private fun rankedList(): TierList = TierList(
        id = 1,
        title = "Sci-fi films",
        tiers = listOf(
            Tier(id = 10, label = "S", colorLight = "#000000", colorDark = "#000000", items = emptyList()),
            Tier(id = 20, label = "A", colorLight = "#000000", colorDark = "#000000", items = emptyList()),
            Tier(id = 30, label = "Pool", colorLight = "#000000", colorDark = "#000000", items = emptyList(), isPool = true),
        ),
    )
}
