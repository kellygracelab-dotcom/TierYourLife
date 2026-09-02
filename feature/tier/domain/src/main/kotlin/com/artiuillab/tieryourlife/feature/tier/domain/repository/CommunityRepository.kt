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

    /**
     * Follows an author, so their new lists come first. Fails for a guest:
     * a guest is an identity this app hands out and later sweeps away, and a
     * following list kept on one would be a promise we delete.
     */
    suspend fun follow(authorUid: String): Result<Unit>

    suspend fun unfollow(authorUid: String): Result<Unit>

    /** Whether this person follows that author, and how many people do. */
    suspend fun followState(authorUid: String): Result<FollowState>

    /**
     * Authors worth following, for somebody who follows nobody yet. Drawn from
     * the lists people have taken most, because that is the only standing
     * anybody has here.
     */
    suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>>

    /**
     * Says that this person took the list to rank for themselves, which is
     * what the popular ordering counts. Counted once per person; saying it
     * twice is harmless and changes nothing.
     */
    suspend fun noteTaken(publishedId: String): Result<Unit>

    /**
     * Everything this person has in the community, as the server has it. Not
     * the same as what this phone remembers publishing: a list survives the
     * phone that published it.
     */
    suspend fun myPublished(): Result<List<PublishedListSummary>>

    suspend fun open(id: String): Result<PublishedList>

    /**
     * Publishes a snapshot and answers with the id it was stored under, along
     * with a record of what was sent, so the board can later tell whether it
     * has moved on. Passing the previous id replaces that snapshot instead of
     * adding another.
     */
    suspend fun publish(list: TierList): Result<Published>

    suspend fun unpublish(publishedId: String): Result<Unit>

    /**
     * Copies one of this person's own pictures somewhere the community can see
     * it, and answers with its address there.
     *
     * A face is shown beside every list they publish, so it cannot live in the
     * folder only they may read. Catalogue art needs none of this -- it has an
     * address already.
     */
    suspend fun makeFace(pictureId: String): Result<String>

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

    /**
     * Takes the list out of the feed for good, and optionally keeps its
     * author from publishing for a while. One request rather than two: there
     * must be no moment in which the list is gone and the author is not yet
     * answered for.
     */
    suspend fun takeDown(publishedId: String, ban: BanLength? = null): Result<Unit>

    /** Closes the complaints about a list and leaves the list alone. */
    suspend fun dismissReports(publishedId: String): Result<Unit>
}

/** What came back from publishing: where it lives, and what went. */
data class Published(val id: String, val fingerprint: String)
