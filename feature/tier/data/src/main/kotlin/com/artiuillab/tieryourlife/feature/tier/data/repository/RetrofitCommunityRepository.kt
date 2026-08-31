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
import com.artiuillab.tieryourlife.feature.tier.data.sync.PictureSync
import com.artiuillab.tieryourlife.feature.tier.data.sync.PublishFingerprint
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
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
    ): Result<CommunityPage> = attempt("Reading the community feed") {
        val page = api.feed(category?.id, query?.takeIf { it.isNotBlank() }, author, after)
        CommunityPage(lists = page.lists.map { it.toSummary() }, nextCursor = page.nextCursor)
    }

    override suspend fun myPublished(): Result<List<PublishedListSummary>> = attempt("Reading what this account has published") {
        api.myPublished().lists.map { it.toSummary() }
    }

    override suspend fun open(id: String): Result<PublishedList> = attempt("Opening a published list") {
        api.open(id).toDomain()
    }

    override suspend fun publish(list: TierList): Result<Published> = try {
        // The server copies a photograph out of this account's folder, so it
        // has to be in that folder first. Sending them here rather than
        // trusting the background trickle: publishing is a button somebody
        // pressed, and blank tiles in the feed would be the only sign that it
        // half worked.
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
        // Sent up first, the same as publishing: the server copies it out of
        // this account's folder, so it has to be in that folder.
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

    override suspend fun takeDown(publishedId: String): Result<Unit> = attempt("Taking a reported list down") {
        api.takeDown(publishedId)
    }

    override suspend fun dismissReports(publishedId: String): Result<Unit> = attempt("Dismissing the reports on a list") {
        api.dismissReports(publishedId)
    }

    private fun ModerationReportDto.toDomain() = ModerationReport(
        listId = listId,
        listTitle = listTitle,
        authorName = authorName,
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
 * Every call here answers with a Result the screen turns into a sentence, and
 * that sentence never says which of the ten it was or why. One line in the log
 * is the difference between "the community is broken" and "App Check refused
 * the token", which is a real evening this cost.
 */
private inline fun <T> attempt(what: String, block: () -> T): Result<T> =
    runCatching(block).onFailure { Timber.w(it, "%s failed", what) }

private fun Throwable.asPublishError(): PublishError = when {
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
    // Null on everything published before the snapshot remembered this, which
    // reads as "the author's arrangement is unknown" rather than as "they
    // ranked nothing".
    arrangement = items.map { it.tierIndex },
)

/**
 * Every picture of this person's own that the board wears, each one once.
 *
 * A poster has an address of its own and is nobody's to upload; a photograph
 * out of the gallery is a file in this app's folder, and its name is the only
 * part of its path worth sending anywhere.
 *
 * Only the tier definitions and the cards travel; where the author put them is
 * theirs, and the reader ranks from scratch.
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
    // missing: the server would look for it, not find it, and the card would
    // end up exactly as bare either way.
    // Numbered against the published tiers, which are the board's minus the
    // pool: a card in the pool is one the author did not rank, and says so
    // with null rather than by pointing at a tier the reader cannot see.
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
