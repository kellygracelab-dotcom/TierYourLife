package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
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
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.Published
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeCommunityRepositoryForDetail(
    private val publishResult: Result<Published> = Result.success(Published("published-1", "sent")),
) : CommunityRepository {
    val published = mutableListOf<TierList>()
    val unpublished = mutableListOf<String>()

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
    ): Result<CommunityPage> = Result.success(CommunityPage(emptyList()))

    override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(emptyList())

    override suspend fun open(id: String): Result<PublishedList> = Result.failure(IllegalStateException())

    override suspend fun publish(list: TierList): Result<Published> {
        published += list
        return publishResult
    }

    override suspend fun unpublish(publishedId: String): Result<Unit> {
        unpublished += publishedId
        return Result.success(Unit)
    }

    override suspend fun makeFace(pictureId: String): Result<String> =
        Result.success("https://example.test/face.jpg")

    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)

    override suspend fun report(
        publishedId: String,
        reason: ReportReason,
        note: String?,
    ): Result<Unit> = Result.success(Unit)
    override suspend fun reports(): Result<List<ModerationReport>> = Result.failure(IllegalStateException())
    override suspend fun takeDown(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun dismissReports(publishedId: String): Result<Unit> = Result.success(Unit)

    override suspend fun follow(authorUid: String): Result<Unit> = Result.success(Unit)

    override suspend fun unfollow(authorUid: String): Result<Unit> = Result.success(Unit)

    override suspend fun followState(authorUid: String): Result<FollowState> =
        Result.success(FollowState(following = false, followers = 0))

    override suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>> = Result.success(emptyList())

    override suspend fun noteTaken(publishedId: String): Result<Unit> = Result.success(Unit)
}

internal class FakeAccountRepositoryForDetail(signedIn: Boolean = false) : AccountRepository {
    override val account: Flow<Account> =
        flowOf(if (signedIn) Account.SignedIn("someone@example.com", null) else Account.Guest)

    override suspend fun signInWithGoogle(idToken: String): SignInOutcome = SignInOutcome.Success

    override suspend fun setDisplayName(name: String): Boolean = true

    override suspend fun setPhotoUrl(photoUrl: String?): Boolean = true

    override suspend fun signOut() = Unit
}
