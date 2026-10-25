package com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker

import com.artiuillab.tieryourlife.feature.community.domain.model.MAX_PENDING_TOPICS
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic

/** What the board will be filed under: nothing, a topic the server knows, or a name it has not seen yet. */
sealed interface TopicChoice {
    data object None : TopicChoice

    data class Known(val topic: Topic) : TopicChoice

    data class New(val name: String) : TopicChoice
}

data class TopicPickerUiState(
    val title: String = "",
    val query: String = "",
    /** Approved topics for the query, or the biggest ones while nothing is typed. */
    val results: List<Topic> = emptyList(),
    val searching: Boolean = false,
    /** Everything this person proposed, the waiting ones first. */
    val mine: List<Topic> = emptyList(),
    /** An approved topic the board's title looks like; a hint, never a block. */
    val suggestion: Topic? = null,
    val chosen: TopicChoice = TopicChoice.None,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val pendingCount: Int get() = mine.count { it.pending }

    val canCreate: Boolean get() = pendingCount < MAX_PENDING_TOPICS

    /** What "create" would name, or null when the field holds nothing fit, or a name that already exists. */
    val createName: String?
        get() {
            val name = cleanTopicName(query) ?: return null
            val known = results + mine + listOfNotNull(suggestion)
            return name.takeUnless { known.any { topic -> topic.name.equals(it, ignoreCase = true) } }
        }

    val pendingLimit: Int get() = MAX_PENDING_TOPICS
}
