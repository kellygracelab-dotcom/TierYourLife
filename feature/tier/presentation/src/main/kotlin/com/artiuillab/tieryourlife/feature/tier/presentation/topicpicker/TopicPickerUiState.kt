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
    /** The board's section; topics are only ever its own. Null until the board is read, or when it has none. */
    val section: String? = null,
    /** The board has been read: a null [section] then means none is chosen yet. */
    val loaded: Boolean = false,
    val query: String = "",
    /** The section's approved topics for the query, or its biggest ones while nothing is typed. */
    val results: List<Topic> = emptyList(),
    val searching: Boolean = false,
    /** Everything this person proposed, the waiting ones first. */
    val mine: List<Topic> = emptyList(),
    /** An approved topic the board's title looks like; a hint, never a block. */
    val suggestion: Topic? = null,
    val chosen: TopicChoice = TopicChoice.None,
    /** The name field Propose a topic opens in place; null while it is closed. */
    val proposal: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val pendingCount: Int get() = mine.count { it.pending }

    /** No section yet: no topics to offer, only the way to choose one. */
    val needsSection: Boolean get() = loaded && section == null

    /** This person's own topics that belong here: the section's, and the waiting ones not filed anywhere yet. */
    val mineHere: List<Topic> get() = mine.filter { it.section == null || it.section == section }

    val canCreate: Boolean get() = pendingCount < MAX_PENDING_TOPICS

    val pendingLimit: Int get() = MAX_PENDING_TOPICS
}
