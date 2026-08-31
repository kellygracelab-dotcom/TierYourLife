package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

interface CommunityRepository {

    /**
     * One page of the published feed. [query] is a prefix match on the title,
     * [author] the uid whose lists to show; both narrow, neither is required.
     * [after] is the cursor the page before handed back.
     */
    suspend fun feed(
        category: ListCategory? = null,
        query: String? = null,
        author: String? = null,
        after: String? = null,
    ): Result<CommunityPage>

    /**
     * Everything this person has in the community, as the server has it. Not
     * the same as what this phone remembers publishing: a list survives the
     * phone that published it.
     */
    suspend fun myPublished(): Result<List<PublishedListSummary>>

    suspend fun open(id: String): Result<PublishedList>

    /**
     * Publishes a snapshot and answers with the id it was stored under. Passing
     * the previous id replaces that snapshot instead of adding another.
     */
    /**
     * Answers with the id the feed keeps it under and a record of what was
     * sent, so the board can later tell whether it has moved on.
     */
    suspend fun publish(list: TierList): Result<Published>

    suspend fun unpublish(publishedId: String): Result<Unit>

    /**
     * Brings the author's name and face on lists they already published up to
     * date. A snapshot freezes what was ranked, not who ranked it.
     */
    suspend fun refreshAuthor(): Result<Unit>

    /** Files a complaint for a person to read. Nothing comes down on its own. */
    suspend fun report(publishedId: String, reason: ReportReason, note: String?): Result<Unit>

    /**
     * Complaints waiting to be read. Fails for everyone but the one person
     * allowed to read them, which is also how the app finds out who that is.
     */
    suspend fun reports(): Result<List<ModerationReport>>

    /** Removes a reported list for everyone and closes its complaints. */
    suspend fun takeDown(publishedId: String): Result<Unit>

    /** Closes the complaints about a list and leaves the list alone. */
    suspend fun dismissReports(publishedId: String): Result<Unit>
}

/** What came back from publishing: where it lives, and what went. */
data class Published(val id: String, val fingerprint: String)
