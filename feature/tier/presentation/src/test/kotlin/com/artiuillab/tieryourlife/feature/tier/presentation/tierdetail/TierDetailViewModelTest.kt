package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PublishedStanding
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

        viewModel.setPublic(true)
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals(1, community.published.size)
        assertEquals("published-1", repository.publishedIds.single().second)
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

        viewModel.setPublic(true)
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

        viewModel.setPublic(true)
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals(listOf(1L to "src-1"), repository.rankingsSet)
        assertEquals(1L to null, repository.publishedIds.last())
        val shown = (viewModel.screen.value.board as TierDetailUiState.Success).list
        assertEquals("src-1", shown.rankingOf)
        assertNull(shown.publishedId)
        assertEquals(UserMessage.PublishedAsRanking, said.await())
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

    /** A board whose new topic name the server took, answering with an id and, when asked about it, with [status]. */
    private suspend fun publishedWithNewTopic(status: TopicStatus): Pair<FakeTierRepository, TierDetailViewModel> {
        val repository = FakeTierRepository(listWithOneCard().copy(newTopicName = "Oscars 2025"))
        val community = FakeCommunityRepositoryForDetail(
            publishResult = Result.success(Published("published-1", "sent", topicId = "t1")),
            topicStatus = status,
        )
        val viewModel = TierDetailViewModel(repository, community, FakeAccountRepository(FakeAccountRepository.signedIn), savedStateHandle())
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        viewModel.setPublic(true)
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
        repository.readsAreHeld = true

        viewModel.setPublic(true)
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

        viewModel.setPublic(true)
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

        viewModel.setPublic(true)

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
        viewModel.setPublic(true)

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
        viewModel.setPublic(true)
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

        viewModel.setPublic(true)
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
        viewModel.setPublic(true)
        viewModel.screen.firstWithin { !it.publish.publishing }

        assertEquals("street-food", community.published.single().category)
    }

    // The whole point of asking: choosing finishes what the switch started, so
    // the second tap is the last one.
    @Test
    fun choosingTheCategoryItAskedFor_finishesThePublish() = runBlocking {
        val community = FakeCommunityRepositoryForDetail()
        val viewModel = TierDetailViewModel(
            FakeTierRepository(listWithOneCard().copy(category = null)),
            community,
            FakeAccountRepository(FakeAccountRepository.signedIn),
            savedStateHandle(),
        )
        viewModel.board.firstWithin { it is TierDetailUiState.Success }
        viewModel.setPublic(true)

        viewModel.setCategory(ListCategory.Food.id)
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
        viewModel.setPublic(true)
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

        viewModel.setPublic(true)

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

        viewModel.setPublic(true)
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

private class FakeTierRepository(
    initial: TierList,
    publishedId: String? = null,
) : TierRepository {

    private var list = initial.copy(publishedId = publishedId)

    val publishedIds = mutableListOf<Pair<Long, String?>>()

    override suspend fun setPublished(id: Long, publishedId: String?, fingerprint: String?) {
        publishedIds += id to publishedId
        list = list.copy(publishedId = publishedId)
    }

    val rankingsSet = mutableListOf<Pair<Long, String?>>()

    override suspend fun setRankingOf(id: Long, listId: String?) {
        rankingsSet += id to listId
        list = list.copy(rankingOf = listId)
    }

    override suspend fun publishedStanding(): PublishedStanding = PublishedStanding()

    override suspend fun boardPublishedAs(publishedId: String): TierList? = null

    override suspend fun setFavouritedAt(id: Long, at: Long?) = Unit

    override suspend fun setCategory(id: Long, category: String?) {
        list = list.copy(category = category)
    }

    override suspend fun setAnonymous(id: Long, anonymous: Boolean) {
        list = list.copy(anonymous = anonymous)
    }

    override suspend fun setPlay(id: Long, play: String?) {
        list = list.copy(play = play)
    }

    override suspend fun setCover(id: Long, pictures: List<String>) {
        list = list.copy(coverImageUrl = pictures.singleOrNull(), coverImageUrls = pictures.takeIf { it.size > 1 }.orEmpty())
    }

    val topicsSet = mutableListOf<List<Any?>>()

    override suspend fun setTopic(id: Long, topicId: String?, topicName: String?, newTopicName: String?) {
        topicsSet += listOf(id, topicId, topicName, newTopicName)
        list = list.copy(topicId = topicId, topicName = topicName, newTopicName = newTopicName)
    }

    override suspend fun createFromTemplate(
        title: String,
        authorName: String,
        tiers: List<Tier>,
        items: List<TierItem>,
        takenFrom: String?,
    ): Long = 0

    private var nextItemId = 1L
    var lastAddedImageUrl: String? = "not yet called"
        private set
    val attachedSources = mutableListOf<String>()
    val permanentlyDeletedIds = mutableListOf<Long>()
    var lastReorderedIds: List<Long>? = null
        private set
    val reorderStarted = CompletableDeferred<Unit>()
    private val reorderGate = CompletableDeferred<Unit>()

    fun releaseReorder() {
        reorderGate.complete(Unit)
    }

    var readsAreHeld = false
    private val heldRead = CompletableDeferred<Unit>()

    override suspend fun getTierListById(id: Long): TierList? {
        if (readsAreHeld) heldRead.await()
        return list
    }

    override suspend fun addItemToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?,
        source: TierItemSource,
    ): Long {
        lastAddedImageUrl = imageUrl
        val id = nextItemId++
        val pool = list.tiers.single { it.isPool }
        val newItem = TierItem(id = id, title = title, imageUrl = imageUrl, source = source)
        val updatedPool = pool.copy(items = pool.items + newItem)
        list = list.copy(tiers = list.tiers.map { if (it.id == pool.id) updatedPool else it })
        return id
    }

    override suspend fun attachImageToItem(itemId: Long, sourceUri: String) {
        attachedSources += sourceUri
    }

    override suspend fun getAllTierLists(): List<TierList> = unsupported()
    override suspend fun createTierList(title: String, captions: List<String>): Long = unsupported()
    override suspend fun setTierListDisplayMode(id: Long, displayMode: TierListDisplayMode) = unsupported()
    override suspend fun renameTierList(id: Long, title: String) = unsupported()
    override suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>) = unsupported()
    override suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) = unsupported()
    override suspend fun addTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
    ): Long = unsupported()

    override suspend fun renameTier(id: Long, label: String, caption: String?) = unsupported()
    override suspend fun updateTierColors(id: Long, colorLight: String, colorDark: String) = unsupported()
    override suspend fun deleteTierToPool(id: Long) = unsupported()
    override suspend fun restoreTier(
        tierListId: Long,
        label: String,
        caption: String?,
        colorLight: String,
        colorDark: String,
        position: Int,
        itemIds: List<Long>,
    ) = unsupported()
    override suspend fun reorderTiers(orderedTierIds: List<Long>) {
        lastReorderedIds = orderedTierIds
        reorderStarted.complete(Unit)
        reorderGate.await()
        val orderedSet = orderedTierIds.toSet()
        val byId = list.tiers.associateBy { it.id }
        val queue = ArrayDeque(orderedTierIds.mapNotNull { byId[it] })
        list = list.copy(tiers = list.tiers.map { tier -> if (tier.id in orderedSet) queue.removeFirst() else tier })
    }
    override suspend fun deleteTierLists(ids: List<Long>) = unsupported()
    override suspend fun restoreTierLists(ids: List<Long>) = unsupported()
    override suspend fun deleteTierItem(id: Long) = unsupported()
    override suspend fun restoreTierItem(id: Long) = unsupported()
    override suspend fun deleteTierListPermanently(id: Long) {
        permanentlyDeletedIds += id
    }
    override suspend fun deleteTierItemPermanently(id: Long) {
        permanentlyDeletedIds += id
        list = list.copy(
            tiers = list.tiers.map { tier -> tier.copy(items = tier.items.filterNot { it.id == id }) },
        )
    }
    override suspend fun emptyTrash() = unsupported()
    override suspend fun getTrashEntries(): List<TrashEntry> = unsupported()

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("Not used by TierDetailViewModel manual-entry tests")
}
