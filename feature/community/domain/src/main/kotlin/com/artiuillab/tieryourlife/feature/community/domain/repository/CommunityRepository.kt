package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.community.domain.model.Moderator
import com.artiuillab.tieryourlife.feature.community.domain.model.PendingTopic
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingsPage
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionLookup
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionNameCheck
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionProposal
import com.artiuillab.tieryourlife.feature.community.domain.model.ShownRanking
import com.artiuillab.tieryourlife.feature.community.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

interface CommunityRepository {

    /**
     * One page of the published feed. [query] is a prefix match on the title,
     * [author] the uid whose lists to show; both narrow, neither is required.
     * [after] is the cursor the page before handed back.
     */
    suspend fun feed(
        /** A section id. */
        category: String? = null,
        query: String? = null,
        author: String? = null,
        after: String? = null,
        sort: FeedSort = FeedSort.Recent,
        following: Boolean = false,
        /** One topic's lists only. */
        topic: String? = null,
    ): Result<CommunityPage>

    /** Fails for a guest: guests are swept away later, and a following list on one would be a promise we delete. */
    suspend fun follow(authorUid: String): Result<Unit>

    suspend fun unfollow(authorUid: String): Result<Unit>

    /** Whether this person follows that author, and how many people do. */
    suspend fun followState(authorUid: String): Result<FollowState>

    /** Whom and what this person follows, with names and faces, for the strip above the feed and the list that unfollows. */
    suspend fun following(): Result<Following>

    /** Approved topics: those starting with [query], or the biggest, within [section] if given. */
    suspend fun topics(query: String? = null, section: String? = null): Result<List<Topic>>

    /** Everything this person proposed, whatever became of it; the waiting ones first. */
    suspend fun myTopics(): Result<List<Topic>>

    suspend fun topic(id: String): Result<Topic>

    /** Every section this person may see, with its counts, in the server's order (§4). Screens read them through [SectionCatalog]. */
    suspend fun sections(): Result<List<Section>>

    /** One section by its id, for a link or a list whose section the kept list does not know. */
    suspend fun section(id: String): Result<SectionLookup>

    suspend fun followSection(id: String): Result<Unit>

    suspend fun unfollowSection(id: String): Result<Unit>

    /** Whether a name is free for a new section, as the server compares names. */
    suspend fun checkSectionName(name: String): Result<SectionNameCheck>

    /** The new section, waiting. Refusals come as [com.artiuillab.tieryourlife.feature.community.domain.model.ProposalRefused]. */
    suspend fun proposeSection(proposal: SectionProposal): Result<Section>

    /** Withdraws a waiting proposal, or puts away the notice of a declined or merged one. */
    suspend fun withdrawProposal(id: String): Result<Unit>

    /** Fails for a guest, like following an author. */
    suspend fun followTopic(id: String): Result<Unit>

    suspend fun unfollowTopic(id: String): Result<Unit>

    /** Tells the server a search found nothing anywhere: only the words are kept, not who typed them. */
    suspend fun reportSearchMiss(query: String): Result<Unit>

    /** The queue of proposed topics. Fails for everyone but a moderator. */
    suspend fun pendingTopics(): Result<List<PendingTopic>>

    /** Places the topic in a section and moves its lists there. */
    suspend fun approveTopic(id: String, section: String): Result<Topic>

    suspend fun renameTopic(id: String, name: String): Result<Topic>

    /** Folds one topic into another, lists and all. */
    suspend fun mergeTopic(id: String, into: String): Result<Topic>

    /** The label disappears from its lists; the lists stay where they are. */
    suspend fun rejectTopic(id: String): Result<Topic>

    /** A moderator moves a list; with a topic, the section is the topic's. */
    suspend fun moveList(publishedId: String, category: String, topicId: String?): Result<Unit>

    /** Fails for everyone but the owner, which is also how the app learns who that is. */
    suspend fun moderators(): Result<List<Moderator>>

    suspend fun appointModerator(uid: String, name: String?, photoUrl: String?): Result<Unit>

    suspend fun removeModerator(uid: String): Result<Unit>

    /** For somebody who follows nobody yet: drawn from the lists people have taken most, the only standing anybody has here. */
    suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>>

    /** What the popular ordering counts. Once per person; saying it twice changes nothing. */
    suspend fun noteTaken(publishedId: String): Result<Unit>

    /** Readers' own placings of this list's cards, newest first, this reader's ahead of the first page. */
    suspend fun rankings(listId: String, after: String? = null): Result<RankingsPage>

    /** Takes this person's ranking of that list down. */
    suspend fun withdrawRanking(listId: String): Result<Unit>

    /**
     * Shows this person's placing of somebody else's list to others, or
     * changes the one shown. [rows] are the tiers' cards left to right, by
     * position among the list's items; [arrangement] says where each card
     * went, for a server that does not read rows yet. A refusal is a RankingRefused.
     */
    suspend fun showMyRanking(
        listId: String,
        arrangement: List<Int?>,
        rows: List<List<Int>>,
        tierLabels: List<String>,
    ): Result<ShownRanking>

    /** As the server has it, not as this phone remembers: a list survives the phone that published it. */
    suspend fun myPublished(): Result<List<PublishedListSummary>>

    /** A list that is not there any more fails with [com.artiuillab.tieryourlife.feature.community.domain.model.ListGone]. */
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

/** What came back from publishing: where it lives, what went, and the topic it was filed under once the server named it. */
data class Published(
    val id: String,
    val fingerprint: String,
    val topicId: String? = null,
    /** True when the server kept it as this person's ranking of [id] rather than as a list of its own. */
    val ranking: Boolean = false,
)
