package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** The publish sheet while it is open; [topics] are the board's section's, null until they come. */
data class PublishSheetState(
    val topics: List<Topic>? = null,
    /** Publish was tapped with no section: the section is what the sheet points at. */
    val sectionWanted: Boolean = false,
    /** Out to the cover picker: the sheet is not drawn over it, and comes back as it was. */
    val away: Boolean = false,
)

/**
 * One sheet for a first publish (Danylo, 16.11.2026): section, then a topic or
 * a proposed one, then Publish. What is chosen goes onto the board at once, so
 * it is still there when the sheet is closed without publishing.
 */
internal class PublishSheetController(
    private val repository: TierRepository,
    private val community: CommunityRepository,
    private val scope: CoroutineScope,
    private val current: () -> TierList?,
    private val reload: suspend () -> Unit,
    private val publish: () -> Unit,
) {
    private val mutable = MutableStateFlow<PublishSheetState?>(null)
    val state: StateFlow<PublishSheetState?> = mutable.asStateFlow()

    private var topicsJob: Job? = null
    private var naming: Job? = null

    fun open() {
        if (mutable.value != null) return
        mutable.value = PublishSheetState()
        current()?.category?.let(::loadTopics)
    }

    fun leaveForCover() {
        mutable.update { it?.copy(away = true) }
    }

    fun back() {
        mutable.update { it?.copy(away = false) }
    }

    fun close() {
        topicsJob?.cancel()
        mutable.value = null
    }

    fun chooseSection(section: String) {
        val list = current() ?: return
        mutable.update { it?.copy(sectionWanted = false, topics = if (section == list.category) it.topics else null) }
        if (section == list.category) return
        scope.launch {
            write("Choosing the section") {
                repository.setCategory(list.id, section)
                // A topic belongs to its section: one chosen under another is not carried over.
                if (list.topicId != null || list.newTopicName != null) repository.setTopic(list.id, null, null, null)
            }
            reload()
        }
        loadTopics(section)
    }

    /** Null takes the topic off, as a second tap on the chosen one does. */
    fun chooseTopic(topic: Topic?) {
        val list = current() ?: return
        scope.launch {
            write("Choosing the topic") {
                if (topic == null) {
                    repository.setTopic(list.id, null, null, null)
                } else {
                    repository.setTopic(list.id, topic.id, topic.name, topic.name.takeIf { topic.pending })
                }
            }
            reload()
        }
    }

    /** A new name goes with the publish and waits for a moderator; until then the list stays in its section. */
    fun proposeTopic(name: String) {
        val list = current() ?: return
        scope.launch {
            write("Proposing a topic") { repository.setTopic(list.id, null, null, name) }
            reload()
        }
    }

    fun publish() {
        val pending = naming?.takeIf { it.isActive }
        if (pending != null) {
            // The name typed a moment ago goes out with the list, not after it.
            scope.launch {
                pending.join()
                publish()
            }
            return
        }
        if (current()?.category == null) {
            mutable.update { it?.copy(sectionWanted = true) }
            return
        }
        publish.invoke()
    }

    /** The name an untitled list is published under, written onto the board first. */
    fun name(title: String) {
        val list = current() ?: return
        if (title.isBlank() || title == list.title) return
        naming = scope.launch {
            write("Naming the list") { repository.renameTierList(list.id, title) }
            reload()
        }
    }

    private fun loadTopics(section: String) {
        topicsJob?.cancel()
        topicsJob = scope.launch {
            val found = community.topics(section = section)
                .onFailure { Timber.w(it, "Reading the section's topics failed") }
                .getOrDefault(emptyList())
            mutable.update { it?.copy(topics = found) }
        }
    }

    private suspend fun write(operation: String, block: suspend () -> Unit) {
        runSuspendCatching { block() }.onFailure { Timber.w(it, "$operation failed") }
    }
}
