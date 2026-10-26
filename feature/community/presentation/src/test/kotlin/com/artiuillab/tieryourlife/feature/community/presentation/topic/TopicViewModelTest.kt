package com.artiuillab.tieryourlife.feature.community.presentation.topic

import androidx.lifecycle.SavedStateHandle
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.presentation.feed.CommunityFeed
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val pokemon = Topic(
    id = "t1",
    name = "Pokémon",
    section = ListCategory.Games,
    status = TopicStatus.Approved,
    listCount = 12,
    followerCount = 3,
)

class TopicViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun theHeader_isRightFromTheRoute_beforeTheServerAnswers() = runBlocking {
        val viewModel = topicFor(TopicFake(answers = false))

        val state = viewModel.state.value

        assertEquals("Pokémon", state.name)
        assertEquals(ListCategory.Games, state.section)
        assertNull(state.topic)
    }

    @Test
    fun theServersTopic_fillsTheCountsIn() = runBlocking {
        val viewModel = topicFor(TopicFake())

        val state = viewModel.state.firstWithin { it.topic != null }

        assertEquals(pokemon, state.topic)
        assertFalse(state.headerFailed)
    }

    // Newest asked, then Popular before it answered: the first answer must not land on top of the second.
    @Test
    fun aSortChosenWhileTheLastOneLoads_isTheOneThatShows() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val community = TopicFake(lists = listOf(published("a")), recentGate = gate)
        val viewModel = topicFor(community)
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.selectSort(FeedSort.Recent)
        viewModel.selectSort(FeedSort.Popular)
        viewModel.state.firstWithin { it.sort == FeedSort.Popular && it.feed is CommunityFeed.Ready }
        gate.complete(Unit)
        delay(SETTLE_MILLIS)

        assertEquals(listOf("a"), (viewModel.state.value.feed as CommunityFeed.Ready).lists.map { it.id })
    }

    @Test
    fun aTopicTheServerWillNotDescribe_stillShowsItsLists() = runBlocking {
        val community = TopicFake(answers = false, lists = listOf(published("a")))
        val viewModel = topicFor(community)

        val state = viewModel.state.firstWithin { it.headerFailed && it.feed is CommunityFeed.Ready }

        assertEquals(listOf("a"), (state.feed as CommunityFeed.Ready).lists.map { it.id })
    }

    @Test
    fun theLists_areTheTopicsOwn_withoutWhatIsHidden() = runBlocking {
        val preferences = FakeAppPreferences().apply { hideList("b", "Hidden one") }
        val community = TopicFake(lists = listOf(published("a"), published("b")))
        val viewModel = topicFor(community, preferences)

        val state = viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        assertEquals(listOf("a"), (state.feed as CommunityFeed.Ready).lists.map { it.id })
        assertEquals(listOf("t1"), community.topicsAsked)
    }

    @Test
    fun itOpensOnPopular_andAnotherOrderAsksAgain() = runBlocking {
        val community = TopicFake(lists = listOf(published("a")))
        val viewModel = topicFor(community)
        viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.selectSort(FeedSort.Recent)
        viewModel.state.firstWithin { it.sort == FeedSort.Recent && it.feed is CommunityFeed.Ready && community.sortsAsked.size == 2 }

        assertEquals(listOf(FeedSort.Popular, FeedSort.Recent), community.sortsAsked)
    }

    @Test
    fun following_answersAtOnce_andTellsTheServer() = runBlocking {
        val community = TopicFake()
        val viewModel = topicFor(community)
        viewModel.state.firstWithin { it.topic != null }

        viewModel.toggleFollow()

        val now = viewModel.state.value.topic!!
        assertTrue(now.following)
        assertEquals(4, now.followerCount)
        assertEquals("t1", withTimeout(WAIT_MILLIS) { community.followAsked.await() })
    }

    @Test
    fun aRefusedFollow_goesBackToHowItWas() = runBlocking {
        val viewModel = topicFor(TopicFake(refuses = true))
        viewModel.state.firstWithin { it.topic != null }

        viewModel.toggleFollow()

        val back = viewModel.state.firstWithin { it.topic?.following == false && it.topic.followerCount == 3 }
        assertEquals(pokemon, back.topic)
    }

    // The list is fine, it is just not of this topic: a moderator moves it, nothing disappears.
    @Test
    fun notThisTopic_isAReportThatHidesNothing() = runBlocking {
        val community = TopicFake(lists = listOf(published("a")))
        val viewModel = topicFor(community)
        val shown = viewModel.state.firstWithin { it.feed is CommunityFeed.Ready }

        viewModel.notThisTopic((shown.feed as CommunityFeed.Ready).lists.single())

        assertEquals("a" to ReportReason.WrongCategory, withTimeout(WAIT_MILLIS) { community.reported.await() })
        assertEquals(listOf("a"), (viewModel.state.value.feed as CommunityFeed.Ready).lists.map { it.id })
    }

    private fun topicFor(community: TopicFake, preferences: FakeAppPreferences = FakeAppPreferences()) = TopicViewModel(
        community = community,
        preferences = preferences,
        savedStateHandle = SavedStateHandle(mapOf("topicId" to "t1", "name" to "Pokémon", "section" to "games")),
    )

    private fun published(id: String) = PublishedListSummary(
        id = id,
        title = "List $id",
        authorUid = "author-$id",
        authorName = "Olena M.",
        category = ListCategory.Games,
        itemCount = 12,
        updatedAtMillis = 0,
        topicId = "t1",
        topicName = "Pokémon",
    )
}

private class TopicFake(
    private val answers: Boolean = true,
    private val lists: List<PublishedListSummary> = emptyList(),
    private val refuses: Boolean = false,
    /** Held while a request for the newest lists is in flight; the rest answer at once. */
    private val recentGate: CompletableDeferred<Unit>? = null,
) : FakeCommunityRepository() {
    val topicsAsked = mutableListOf<String?>()
    val sortsAsked = mutableListOf<FeedSort>()
    val followAsked = CompletableDeferred<String>()
    val reported = CompletableDeferred<Pair<String, ReportReason>>()

    override suspend fun topic(id: String): Result<Topic> =
        if (answers) Result.success(pokemon) else Result.failure(IllegalStateException("offline"))

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
        topic: String?,
    ): Result<CommunityPage> {
        topicsAsked += topic
        sortsAsked += sort
        if (sort == FeedSort.Recent && recentGate != null) {
            recentGate.await()
            return Result.success(CommunityPage(emptyList()))
        }
        return Result.success(CommunityPage(lists))
    }

    override suspend fun followTopic(id: String): Result<Unit> {
        followAsked.complete(id)
        return if (refuses) Result.failure(IllegalStateException("offline")) else Result.success(Unit)
    }

    override suspend fun report(publishedId: String, reason: ReportReason, note: String?): Result<Unit> {
        reported.complete(publishedId to reason)
        return Result.success(Unit)
    }
}

private const val WAIT_MILLIS = 5_000L
private const val SETTLE_MILLIS = 200L
