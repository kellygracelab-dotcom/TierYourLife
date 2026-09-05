package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.ModerationReportDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishListRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedItemDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedListSummaryDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedTierDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.ReportRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.TakeDownRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.isAppUnverified
import com.artiuillab.tieryourlife.feature.tier.data.sync.PictureSync
import com.artiuillab.tieryourlife.feature.tier.data.sync.PublishFingerprint
import com.artiuillab.tieryourlife.feature.tier.domain.model.AppUnverified
import com.artiuillab.tieryourlife.feature.tier.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.Published
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitCommunityRepository @Inject constructor(
    private val api: CommunityApi,
    private val images: TierImageStore,
    private val pictures: PictureSync,
) : CommunityRepository {

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
    ): Result<CommunityPage> = attempt("Reading the community feed") {
        val page = api.feed(
            category = category?.id,
            query = query?.takeIf { it.isNotBlank() },
            author = author,
            after = after,
            sort = sort.id,
            // Sent only when wanted: an ordinary feed carries no parameter
            // the server has to read as false.
            following = "1".takeIf { following },
        )
        CommunityPage(
            lists = page.lists.map { it.toSummary() },
            nextCursor = page.nextCursor,
            followingNobody = page.followingNobody,
        )
    }

    override suspend fun follow(authorUid: String): Result<Unit> = attempt("Following an author") {
        api.follow(authorUid)
    }

    override suspend fun unfollow(authorUid: String): Result<Unit> = attempt("Unfollowing an author") {
        api.unfollow(authorUid)
    }

    override suspend fun followState(authorUid: String): Result<FollowState> =
        attempt("Reading whether an author is followed") {
            val state = api.followState(authorUid)
            FollowState(following = state.following, followers = state.followers)
        }

    override suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>> =
        attempt("Reading who to follow") {
            api.suggestedAuthors().authors.map {
                SuggestedAuthor(uid = it.uid, name = it.name, photoUrl = it.photoUrl, takeCount = it.takeCount)
            }
        }

    override suspend fun noteTaken(publishedId: String): Result<Unit> = attempt("Counting a list as taken") {
        api.noteTaken(publishedId)
    }

    override suspend fun myPublished(): Result<List<PublishedListSummary>> = attempt("Reading what this account has published") {
        api.myPublished().lists.map { it.toSummary() }
    }

    override suspend fun open(id: String): Result<PublishedList> = attempt("Opening a published list") {
        api.open(id).toDomain()
    }

    override suspend fun publish(list: TierList): Result<Published> = try {
        // The server copies pictures out of this account's folder, so they
        // must be there first. Sent now rather than by the background trickle:
        // publishing is a button somebody pressed.
        val up = pictures.sendNow(list.ownPictureIds(images))
        val request = list.toRequest(images, up)
        val existing = list.publishedId
        val id = if (existing == null) api.publish(request).id else api.republish(existing, request).id
        Result.success(Published(id = id, fingerprint = PublishFingerprint.of(list, images::pictureIdOf)))
    } catch (e: Exception) {
        Timber.w(e, "Publishing failed")
        Result.failure(PublishRefused(e.asPublishError()))
    }

    override suspend fun unpublish(publishedId: String): Result<Unit> = attempt("Taking a list back down") {
        api.unpublish(publishedId)
    }

    override suspend fun makeFace(pictureId: String): Result<String> = attempt("Making that picture a face") {
        // Sent up first, as for publishing.
        pictures.sendNow(listOf(pictureId))
        api.makeFace(pictureId).url
    }

    override suspend fun refreshAuthor(): Result<Unit> = attempt("Refreshing the author on published lists") { api.refreshAuthor() }

    override suspend fun report(
        publishedId: String,
        reason: ReportReason,
        note: String?,
    ): Result<Unit> = attempt("Filing a report") {
        api.report(publishedId, ReportRequestDto(reason.id, note?.takeIf { it.isNotBlank() }))
    }

    override suspend fun reports(): Result<List<ModerationReport>> = attempt("Reading the report queue") {
        api.reports().reports.map { it.toDomain() }
    }

    override suspend fun takeDown(publishedId: String, ban: BanLength?): Result<Unit> =
        attempt("Taking a reported list down") {
            api.takeDown(publishedId, TakeDownRequestDto(ban?.id))
        }

    override suspend fun dismissReports(publishedId: String): Result<Unit> = attempt("Dismissing the reports on a list") {
        api.dismissReports(publishedId)
    }

    private fun ModerationReportDto.toDomain() = ModerationReport(
        listId = listId,
        listTitle = listTitle,
        authorName = authorName,
        authorUid = authorUid,
        authorPhotoUrl = authorPhotoUrl,
        coverImageUrl = coverImageUrl,
        // A reason we do not recognise is still a complaint worth reading.
        reasons = reasons.map { given ->
            ReportReason.entries.firstOrNull { it.id == given } ?: ReportReason.Other
        },
        notes = notes,
        reportCount = reportCount,
        newestAtMillis = newestAtMs,
        hidden = hidden,
        reviewed = reviewed,
    )
}

/**
 * Every Result becomes one sentence on a screen; the log line is the only
 * place that says which of the ten calls failed, and why.
 */
private inline fun <T> attempt(what: String, block: () -> T): Result<T> =
    runCatching(block).fold(
        onSuccess = { Result.success(it) },
        onFailure = { failure ->
            Timber.w(failure, "%s failed", what)
            // Named here rather than at each screen: every caller of this file
            // gets the same refusal, and none of them can read an HTTP body.
            Result.failure(if (failure.isAppUnverified()) AppUnverified() else failure)
        },
    )

private fun Throwable.asPublishError(): PublishError = when {
    isAppUnverified() -> PublishError.NotVerified
    this is HttpException -> when (code()) {
        403 -> PublishError.NotSignedIn
        409 -> PublishError.TooManyLists
        413 -> PublishError.TooLarge
        422 -> PublishError.PictureRefused
        else -> PublishError.Unknown
    }

    this is IOException -> PublishError.Offline
    else -> PublishError.Unknown
}

private fun PublishedListSummaryDto.toSummary() = PublishedListSummary(
    id = id,
    title = title,
    authorUid = authorUid,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    category = ListCategory.fromId(category) ?: ListCategory.Other,
    itemCount = itemCount,
    coverImageUrl = coverImageUrl,
    previewImages = previewImages,
    tierColors = tierColors,
    updatedAtMillis = updatedAt,
    takeCount = takeCount,
)

private fun PublishedListDto.toDomain() = PublishedList(
    summary = PublishedListSummary(
        id = id,
        title = title,
        authorUid = authorUid,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        category = ListCategory.fromId(category) ?: ListCategory.Other,
        itemCount = itemCount,
        coverImageUrl = coverImageUrl,
        previewImages = previewImages,
        tierColors = tierColors,
        updatedAtMillis = updatedAt,
    ),
    tiers = tiers.mapIndexed { index, tier ->
        Tier(
            id = index.toLong(),
            label = tier.label,
            caption = tier.caption,
            colorLight = tier.colorLight,
            colorDark = tier.colorDark,
            items = emptyList(),
        )
    },
    items = items.mapIndexed { index, item ->
        TierItem(id = index.toLong(), title = item.title, imageUrl = item.imageUrl)
    },
    // Null on snapshots published before this was remembered: "arrangement
    // unknown", not "ranked nothing".
    arrangement = items.map { it.tierIndex },
)

/**
 * This person's own pictures, each once: a poster has its own address, a
 * gallery photo is a file whose name is the only part worth sending.
 */
private fun TierList.ownPictureIds(images: TierImageStore): List<String> =
    (tiers.flatMap { tier -> tier.items.map { it.imageUrl } } + coverImageUrl)
        .mapNotNull(images::pictureIdOf)
        .distinct()

private fun TierList.toRequest(images: TierImageStore, uploaded: Set<String>) = PublishListRequestDto(
    title = title,
    category = (category ?: ListCategory.Other).id,
    coverImageUrl = coverImageUrl?.takeIf { it.startsWith("https://") },
    coverPictureId = images.pictureIdOf(coverImageUrl)?.takeIf { it in uploaded },
    tiers = tiers.filterNot { it.isPool }.map {
        PublishedTierDto(it.label, it.caption, it.colorLight, it.colorDark)
    },
    // A picture that would not upload is left unnamed rather than named and
    // missing. Tier indexes count the published tiers (the board's minus the
    // pool); a card in the pool says so with null.
    items = run {
        val ranked = tiers.filterNot { it.isPool }
        tiers.flatMap { tier ->
            val where = ranked.indexOfFirst { it.id == tier.id }.takeIf { it >= 0 }
            tier.items.map { item ->
                PublishedItemDto(
                    title = item.title,
                    imageUrl = item.imageUrl?.takeIf { it.startsWith("https://") },
                    pictureId = images.pictureIdOf(item.imageUrl)?.takeIf { it in uploaded },
                    tierIndex = where,
                )
            }
        }
    },
)
