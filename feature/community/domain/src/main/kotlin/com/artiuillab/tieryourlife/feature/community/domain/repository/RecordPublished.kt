package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository

/**
 * What a publish leaves on the board once out, written whether or not a
 * screen is still there to see it: the id it went out as, the topic the
 * server named, and the version of the list a copy answers.
 */
class RecordPublished(
    private val boards: TierRepository,
    private val community: CommunityRepository,
    /** Whether the list waits for approval before the feed, by its id. */
    private val awaitingApproval: (id: String, waiting: Boolean) -> Unit = { _, _ -> },
) {
    suspend operator fun invoke(sent: TierList, published: Published, originVersion: Long?) {
        if (sent.takenFrom != null) {
            originVersion?.let { boards.setRankingOrigin(sent.id, it) }
            boards.setRankingChoice(sent.id, RankingChoice.Ranking)
        }
        // A ranking has no id of its own: it is out as part of the list it answers.
        boards.setPublished(sent.id, published.takeUnless { it.ranking }?.id, published.fingerprint)
        boards.setRankingOf(sent.id, published.takeIf { it.ranking }?.id)
        val topicId = published.topicId
        if (topicId != null) {
            // The server named the topic: a name that waited is a topic now, though maybe one still waiting.
            boards.setTopic(sent.id, topicId, published.topicName ?: sent.topicName ?: sent.newTopicName, waitingName(sent, topicId))
        } else if (sent.topicTakenOff) {
            // Said once and done: from now on no topic here leaves the list's as it is.
            boards.setTopic(sent.id, null, null, null)
        }
        if (!published.ranking) awaitingApproval(published.id, published.awaitingApproval)
    }

    private suspend fun waitingName(list: TierList, topicId: String): String? {
        val proposed = list.newTopicName?.takeIf { list.topicId == null } ?: return null
        val settled = community.topic(topicId).map { it.status != TopicStatus.Pending }.getOrDefault(false)
        return proposed.takeUnless { settled }
    }
}
