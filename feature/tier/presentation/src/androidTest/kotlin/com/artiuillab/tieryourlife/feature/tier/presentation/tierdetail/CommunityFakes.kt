package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeCommunityRepositoryForDetail(
    private val publishResult: Result<String> = Result.success("published-1"),
) : CommunityRepository {
    val published = mutableListOf<TierList>()
    val unpublished = mutableListOf<String>()

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
    ): Result<CommunityPage> = Result.success(CommunityPage(emptyList()))

    override suspend fun open(id: String): Result<PublishedList> = Result.failure(IllegalStateException())

    override suspend fun publish(list: TierList): Result<String> {
        published += list
        return publishResult
    }

    override suspend fun unpublish(publishedId: String): Result<Unit> {
        unpublished += publishedId
        return Result.success(Unit)
    }

    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)

    override suspend fun report(
        publishedId: String,
        reason: ReportReason,
        note: String?,
    ): Result<Unit> = Result.success(Unit)
}

internal class FakeAccountRepositoryForDetail(signedIn: Boolean = false) : AccountRepository {
    override val account: Flow<Account> =
        flowOf(if (signedIn) Account.SignedIn("someone@example.com", null) else Account.Guest)

    override suspend fun signInWithGoogle(idToken: String): SignInOutcome = SignInOutcome.Success

    override suspend fun setDisplayName(name: String): Boolean = true

    override suspend fun setPhotoUrl(photoUrl: String?): Boolean = true

    override fun googlePhotoUrl(): String? = null

    override suspend fun signOut() = Unit
}
