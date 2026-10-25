package com.artiuillab.tieryourlife.feature.community.domain

import com.artiuillab.tieryourlife.feature.community.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/**
 * A quiet community seen by an ordinary person: empty pages and lists, every
 * write accepted, and the report queue refused because they are no moderator.
 * A test overrides only the calls it is about. Opening or publishing a list
 * fails until a test says what it should find; there is no sensible default.
 */
open class FakeCommunityRepository : CommunityRepository {

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
    ): Result<CommunityPage> = Result.success(CommunityPage(emptyList()))

    override suspend fun follow(authorUid: String): Result<Unit> = Result.success(Unit)

    override suspend fun unfollow(authorUid: String): Result<Unit> = Result.success(Unit)

    override suspend fun followState(authorUid: String): Result<FollowState> =
        Result.success(FollowState(following = false, followers = 0))

    override suspend fun following(): Result<List<FollowedAuthor>> = Result.success(emptyList())

    override suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>> = Result.success(emptyList())

    override suspend fun noteTaken(publishedId: String): Result<Unit> = Result.success(Unit)

    override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(emptyList())

    override suspend fun open(id: String): Result<PublishedList> = notSetUp("open")

    override suspend fun publish(list: TierList): Result<Published> = notSetUp("publish")

    override suspend fun unpublish(publishedId: String): Result<Unit> = Result.success(Unit)

    override suspend fun makeFace(pictureId: String): Result<String> = Result.success(FACE_URL)

    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)

    override suspend fun report(publishedId: String, reason: ReportReason, note: String?): Result<Unit> =
        Result.success(Unit)

    override suspend fun reports(): Result<List<ModerationReport>> =
        Result.failure(IllegalStateException("Only a moderator reads the report queue"))

    override suspend fun takeDown(publishedId: String, ban: BanLength?): Result<Unit> = Result.success(Unit)

    override suspend fun dismissReports(publishedId: String): Result<Unit> = Result.success(Unit)

    private fun <T> notSetUp(call: String): Result<T> =
        Result.failure(IllegalStateException("FakeCommunityRepository.$call was not set up by this test"))

    companion object {
        const val FACE_URL = "https://example.test/face.jpg"
    }
}
