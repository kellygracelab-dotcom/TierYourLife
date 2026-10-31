package com.artiuillab.tieryourlife.feature.community.domain.model

/** Where a proposed word stands: waiting for a moderator, admitted, turned down, or folded into another. */
enum class TopicStatus(val id: String) {
    Pending("pending"),
    Approved("approved"),
    Rejected("rejected"),
    Merged("merged"),
    ;

    companion object {
        fun fromId(id: String?): TopicStatus = entries.firstOrNull { it.id == id } ?: Pending
    }
}

/** A word of somebody's own inside a section: "Pokémon" under Games. */
data class Topic(
    val id: String,
    val name: String,
    /** A section id; null while the topic waits: its lists sit under Other until a moderator places it. */
    val section: String?,
    val status: TopicStatus,
    val followerCount: Int = 0,
    val following: Boolean = false,
    val createdBy: String = "",
    val mergedInto: String? = null,
    /** Its own square picture; without one the section's tinted square stands in. */
    val pictureUrl: String? = null,
) {
    val pending: Boolean get() = status == TopicStatus.Pending
}

/** A topic in the queue with what a decision needs: who asked, where they wanted it, and what it might be a twin of. */
data class PendingTopic(
    val topic: Topic,
    val proposerName: String,
    val requestedSection: String?,
    val similar: List<Topic> = emptyList(),
)

/** Somebody the owner lets decide. */
data class Moderator(
    val uid: String,
    val name: String,
    val photoUrl: String?,
    val appointedAtMillis: Long,
)

/** How many topics one account may have waiting at once. */
const val MAX_PENDING_TOPICS = 3
