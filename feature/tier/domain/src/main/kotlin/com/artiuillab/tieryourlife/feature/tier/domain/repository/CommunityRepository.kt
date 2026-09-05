package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.SuggestedAuthor
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
        sort: FeedSort = FeedSort.Recent,
        following: Boolean = false,
    ): Result<CommunityPage>

    /** Fails for a guest: guests are swept away later, and a following list on one would be a promise we delete. */
    suspend fun follow(authorUid: String): Result<Unit>

    suspend fun unfollow(authorUid: String): Result<Unit>

    /** Whether this person follows that author, and how many people do. */
    suspend fun followState(authorUid: String): Result<FollowState>

    /** For somebody who follows nobody yet: drawn from the lists people have taken most, the only standing anybody has here. */
    suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>>

    /** What the popular ordering counts. Once per person; saying it twice changes nothing. */
    suspend fun noteTaken(publishedId: String): Result<Unit>

    /** As the server has it, not as this phone remembers: a list survives the phone that published it. */
    suspend fun myPublished(): Result<List<PublishedListSummary>>

    suspend fun open(id: String): Result<PublishedList>

    /**
     * Answers with the stored id and a record of what was sent, so the board can
     * tell later whether it moved on. A board with a previous id replaces that snapshot.
     */
    suspend fun publish(list: TierList): Result<Published>

    suspend fun unpublish(publishedId: String): Result<Unit>

    /** Copies one of this person's own pictures where the community can see it: a face cannot live in the folder only they may read. */
    suspend fun makeFace(pictureId: String): Result<String>

    /** A snapshot freezes what was ranked, not who ranked it. */
    suspend fun refreshAuthor(): Result<Unit>

    /** Files a complaint for a person to read. Nothing comes down on its own. */
    suspend fun report(publishedId: String, reason: ReportReason, note: String?): Result<Unit>

    /**
     * Complaints waiting to be read. Fails for everyone but the one person
     * allowed to read them, which is also how the app finds out who that is.
     */
    suspend fun reports(): Result<List<ModerationReport>>

    /** One request, not two: no moment in which the list is gone and the author not yet answered for. */
    suspend fun takeDown(publishedId: String, ban: BanLength? = null): Result<Unit>

    /** Closes the complaints about a list and leaves the list alone. */
    suspend fun dismissReports(publishedId: String): Result<Unit>
}

/** What came back from publishing: where it lives, and what went. */
data class Published(val id: String, val fingerprint: String)
