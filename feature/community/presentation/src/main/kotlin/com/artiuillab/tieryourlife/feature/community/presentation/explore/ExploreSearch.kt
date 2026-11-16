package com.artiuillab.tieryourlife.feature.community.presentation.explore

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What a search shows instead of the tabs: the server's answer to what was typed. */
data class SearchResults(
    val lists: List<PublishedListSummary> = emptyList(),
    /** Approved topics whose name starts with what was typed. */
    val topics: List<Topic> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    /** Where the next page of lists starts; null when the server has no more. */
    val cursor: String? = null,
    val loadingMore: Boolean = false,
) {
    val canLoadMore: Boolean get() = cursor != null && !loading && !failed

    /** An answer, and an empty one: not a search still asked or one that failed. */
    val foundNothing: Boolean get() = !loading && !failed && lists.isEmpty() && topics.isEmpty()
}

private const val SEARCH_DELAY_MILLIS = 300L

/** As many as the empty search has room for above the keyboard. */
private const val POPULAR_TOPICS = 4

/** Where a search looks when it is not everywhere. */
sealed interface SearchScope {
    /** The section's topics and lists. */
    data class Section(val id: String) : SearchScope

    /** The topic's lists. */
    data class Topic(val id: String) : SearchScope
}

/** Explore's search: the server's answer to what was typed, paged on like the website's (SPEC list-flow К6). */
class ExploreSearch(
    private val community: CommunityRepository,
    private val scope: CoroutineScope,
    private val visible: (PublishedListSummary) -> Boolean,
    private val sectionMatches: (String) -> Boolean,
) {
    private val _query = MutableStateFlow<String?>(null)

    /** Non-null while the search bar is up. */
    val query: StateFlow<String?> = _query.asStateFlow()

    private val _results = MutableStateFlow(SearchResults())
    val results: StateFlow<SearchResults> = _results.asStateFlow()

    private val _within = MutableStateFlow<SearchScope?>(null)

    /** Where the search looks; null for everywhere. */
    val within: StateFlow<SearchScope?> = _within.asStateFlow()

    private val _popularTopics = MutableStateFlow<List<Topic>>(emptyList())

    /** What a search everywhere offers before anything is typed: the biggest approved topics, whatever their section. */
    val popularTopics: StateFlow<List<Topic>> = _popularTopics.asStateFlow()

    private var job: Job? = null

    fun enter(within: SearchScope? = null) {
        _within.value = within
        _query.value = ""
        _results.value = SearchResults()
        if (within == null && _popularTopics.value.isEmpty()) {
            scope.launch {
                community.topics().onSuccess { topics ->
                    _popularTopics.value = topics.filter { it.status == TopicStatus.Approved }.take(POPULAR_TOPICS)
                }
            }
        }
    }

    /** From one section or topic to everywhere, asking again for what is typed. */
    fun widen() {
        if (_within.value == null) return
        _within.value = null
        retry()
    }

    /** A request, not a filter over what is on screen: it waits for the typing to settle. */
    fun update(typed: String) {
        _query.value = typed
        job?.cancel()
        if (typed.isBlank()) {
            _results.value = SearchResults()
            return
        }
        _results.value = _results.value.copy(loading = true, failed = false)
        val within = _within.value
        job = scope.launch {
            delay(SEARCH_DELAY_MILLIS)
            val topics = async {
                if (within is SearchScope.Topic) return@async emptyList()
                community.topics(query = typed, section = within.section).getOrDefault(emptyList()).filter { it.status == TopicStatus.Approved }
            }
            val lists = community.feed(category = within.section, topic = within.topic, query = typed)
            val found = topics.await()
            _results.value = lists.fold(
                onSuccess = { page -> SearchResults(lists = page.lists.filter(visible), topics = found, cursor = page.nextCursor) },
                // Topics found are still an answer when the lists could not be read.
                onFailure = { if (found.isEmpty()) SearchResults(failed = true) else SearchResults(topics = found) },
            )
            if (within == null && _results.value.foundNothing && !sectionMatches(typed)) community.reportSearchMiss(typed)
        }
    }

    /** The next page, with lists already shown dropped by id and the server's order kept. */
    fun loadMore() {
        val typed = _query.value ?: return
        val now = _results.value
        val after = now.cursor ?: return
        if (!now.canLoadMore || now.loadingMore) return
        _results.value = now.copy(loadingMore = true)
        val within = _within.value
        job = scope.launch {
            community.feed(category = within.section, topic = within.topic, query = typed, after = after).fold(
                onSuccess = { page ->
                    val known = _results.value.lists.mapTo(mutableSetOf()) { it.id }
                    val more = page.lists.filter { it.id !in known && visible(it) }
                    _results.value = _results.value.copy(lists = _results.value.lists + more, cursor = page.nextCursor, loadingMore = false)
                },
                onFailure = { _results.value = _results.value.copy(loadingMore = false) },
            )
        }
    }

    fun retry() {
        _query.value?.let(::update)
    }

    fun exit() {
        job?.cancel()
        _query.value = null
        _within.value = null
        _results.value = SearchResults()
    }
}

private val SearchScope?.section: String? get() = (this as? SearchScope.Section)?.id

private val SearchScope?.topic: String? get() = (this as? SearchScope.Topic)?.id
