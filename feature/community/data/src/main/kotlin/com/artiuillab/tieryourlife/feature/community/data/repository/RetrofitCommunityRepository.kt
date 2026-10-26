package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.community.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.community.data.remote.banNotice
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.AppointModeratorRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.BasedOnDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.FollowedSectionDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ModerationReportDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.MoveListRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PendingTopicDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishListRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedItemDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListSummaryDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedTierDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.RankingDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ReportRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TakeDownRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TopicDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TopicMergeRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TopicNameRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TopicSectionRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.isAppUnverified
import com.artiuillab.tieryourlife.feature.community.data.remote.proxyCode
import com.artiuillab.tieryourlife.feature.community.domain.model.AppUnverified
import com.artiuillab.tieryourlife.feature.community.domain.model.BanLength
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.EditorialAuthors
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedSection
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.community.domain.model.Moderator
import com.artiuillab.tieryourlife.feature.community.domain.model.PendingTopic
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingsPage
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStats
import com.artiuillab.tieryourlife.feature.community.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PublishFingerprint
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitCommunityRepository @Inject constructor(
    private val api: CommunityApi,
    private val pictures: OwnPictures,
) : CommunityRepository {

    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
        topic: String?,
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
            topic = topic,
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

    override suspend fun following(): Result<Following> =
        attempt("Reading who and what is followed") {
            val answer = api.following()
            Following(
                authors = answer.authors.map { FollowedAuthor(uid = it.uid, name = it.name, photoUrl = it.photoUrl) },
                topics = answer.topics.map { it.toDomain() },
                sections = answer.sections.mapNotNull { it.toDomain() },
            )
        }

    /** The last answer, so the warm-up at start is what Explore opens with. */
    @Volatile
    private var lastSections: List<SectionStats>? = null

    override suspend fun sections(): Result<List<SectionStats>> = attempt("Reading the sections") {
        sectionsInOrder(api.sections().sections).also { lastSections = it }
    }.recoverCatching { error -> lastSections ?: throw error }

    override suspend fun followSection(section: ListCategory): Result<Unit> = attempt("Following a section") {
        api.followSection(section.id)
        lastSections = lastSections?.map { if (it.section == section) it.followedBy(true) else it }
    }

    override suspend fun unfollowSection(section: ListCategory): Result<Unit> = attempt("Unfollowing a section") {
        api.unfollowSection(section.id)
        lastSections = lastSections?.map { if (it.section == section) it.followedBy(false) else it }
    }

    override suspend fun warmUp(): Result<Unit> = sections().map { }

    override suspend fun topics(query: String?, section: ListCategory?): Result<List<Topic>> =
        attempt("Searching topics") {
            api.topics(query = query?.trim()?.takeIf { it.isNotEmpty() }, section = section?.id).topics.map { it.toDomain() }
        }

    override suspend fun myTopics(): Result<List<Topic>> =
        attempt("Reading this person's topics") { api.myTopics().topics.map { it.toDomain() } }

    override suspend fun topic(id: String): Result<Topic> = attempt("Reading a topic") { api.topic(id).toDomain() }

    override suspend fun followTopic(id: String): Result<Unit> = attempt("Following a topic") { api.followTopic(id) }

    override suspend fun unfollowTopic(id: String): Result<Unit> = attempt("Unfollowing a topic") { api.unfollowTopic(id) }

    override suspend fun pendingTopics(): Result<List<PendingTopic>> =
        attempt("Reading the topic queue") { api.pendingTopics().topics.map { it.toDomain() } }

    override suspend fun approveTopic(id: String, section: ListCategory): Result<Topic> =
        attempt("Approving a topic") { api.approveTopic(id, TopicSectionRequestDto(section.id)).toDomain() }

    override suspend fun renameTopic(id: String, name: String): Result<Topic> =
        attempt("Renaming a topic") { api.renameTopic(id, TopicNameRequestDto(name.trim())).toDomain() }

    override suspend fun mergeTopic(id: String, into: String): Result<Topic> =
        attempt("Merging a topic") { api.mergeTopic(id, TopicMergeRequestDto(into)).toDomain() }

    override suspend fun rejectTopic(id: String): Result<Topic> = attempt("Rejecting a topic") { api.rejectTopic(id).toDomain() }

    override suspend fun moveList(publishedId: String, category: ListCategory, topicId: String?): Result<Unit> =
        attempt("Moving a list") { api.moveList(publishedId, MoveListRequestDto(category.id, topicId)) }

    override suspend fun moderators(): Result<List<Moderator>> = attempt("Reading the moderators") {
        api.moderators().moderators.map { Moderator(uid = it.uid, name = it.name, photoUrl = it.photoUrl, appointedAtMillis = it.appointedAt) }
    }

    override suspend fun appointModerator(uid: String, name: String?, photoUrl: String?): Result<Unit> =
        attempt("Appointing a moderator") { api.appointModerator(uid, AppointModeratorRequestDto(name, photoUrl)) }

    override suspend fun removeModerator(uid: String): Result<Unit> =
        attempt("Removing a moderator") { api.removeModerator(uid) }

    override suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>> =
        attempt("Reading who to follow") {
            api.suggestedAuthors().authors
                // Nobody to follow behind a list that stands without an author.
                .filterNot { EditorialAuthors.isEditorial(it.uid) }
                .map {
                    SuggestedAuthor(uid = it.uid, name = it.name, photoUrl = it.photoUrl, takeCount = it.takeCount)
                }
        }

    override suspend fun noteTaken(publishedId: String): Result<Unit> = attempt("Counting a list as taken") {
        api.noteTaken(publishedId)
    }

    override suspend fun rankings(listId: String, after: String?): Result<RankingsPage> = attempt("Reading a list's rankings") {
        val page = api.rankings(listId, after)
        RankingsPage(rankings = page.rankings.map { it.toDomain() }, nextCursor = page.nextCursor)
    }

    override suspend fun withdrawRanking(listId: String): Result<Unit> = attempt("Taking a ranking down") {
        api.deleteMyRanking(listId)
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
        val up = pictures.sendNow(list.ownPictureIds(pictures))
        val request = list.toRequest(pictures, up)
        val existing = list.publishedId
        val answer = if (existing == null) api.publish(request) else api.republish(existing, request)
        Result.success(
            Published(
                id = answer.id,
                fingerprint = PublishFingerprint.of(list, pictures::pictureIdOf),
                topicId = answer.topicId,
                ranking = answer.ranking,
            ),
        )
    } catch (e: CancellationException) {
        throw e
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
    runSuspendCatching(block).fold(
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
        HTTP_FORBIDDEN -> banNotice()?.let { PublishError.Banned(it.untilMillis) } ?: PublishError.NotSignedIn
        HTTP_CONFLICT -> if (proxyCode() == TOO_MANY_PENDING_TOPICS) PublishError.TooManyPendingTopics else PublishError.TooManyLists
        HTTP_BAD_REQUEST -> if (proxyCode() == TOPIC_UNAVAILABLE) PublishError.TopicUnavailable else PublishError.Unknown
        HTTP_TOO_LARGE -> PublishError.TooLarge
        HTTP_UNPROCESSABLE -> PublishError.PictureRefused
        else -> PublishError.Unknown
    }

    this is IOException -> PublishError.Offline
    else -> PublishError.Unknown
}

private const val TOO_MANY_PENDING_TOPICS = "TOO_MANY_PENDING_TOPICS"
private const val TOPIC_UNAVAILABLE = "TOPIC_UNAVAILABLE"
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_FORBIDDEN = 403
private const val HTTP_CONFLICT = 409
private const val HTTP_TOO_LARGE = 413
private const val HTTP_UNPROCESSABLE = 422

private fun TopicDto.toDomain() = Topic(
    id = id,
    name = name,
    section = ListCategory.fromId(section),
    status = TopicStatus.fromId(status),
    listCount = listCount,
    followerCount = followerCount,
    following = following,
    createdBy = createdBy,
    mergedInto = mergedInto,
)

private fun PendingTopicDto.toDomain() = PendingTopic(
    topic = Topic(
        id = id,
        name = name,
        section = ListCategory.fromId(section),
        status = TopicStatus.fromId(status),
        listCount = listCount,
        followerCount = followerCount,
        following = following,
        createdBy = createdBy,
        mergedInto = mergedInto,
    ),
    proposerName = proposerName,
    requestedSection = ListCategory.fromId(requestedSection),
    similar = similar.map { it.toDomain() },
)

internal fun PublishedListSummaryDto.toSummary() = PublishedListSummary(
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
    anonymous = anonymous,
    topicId = topicId,
    topicName = topicName,
    rankingCount = rankingCount,
    basedOn = basedOn?.toDomain(),
)

internal fun PublishedListDto.toDomain() = PublishedList(
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
        anonymous = anonymous,
        topicId = topicId,
        topicName = topicName,
        rankingCount = rankingCount,
        basedOn = basedOn?.toDomain(),
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
private fun TierList.ownPictureIds(images: OwnPictures): List<String> =
    (tiers.flatMap { tier -> tier.items.map { it.imageUrl } } + coverImageUrl)
        .mapNotNull(images::pictureIdOf)
        .distinct()

internal fun TierList.toRequest(images: OwnPictures, uploaded: Set<String>) = PublishListRequestDto(
    title = title,
    category = (category ?: ListCategory.Other).id,
    coverImageUrl = coverImageUrl?.takeIf { it.startsWith("https://") },
    coverPictureId = images.pictureIdOf(coverImageUrl)?.takeIf { it in uploaded },
    coverImageUrls = coverImageUrls.filter { it.startsWith("https://") },
    topicId = topicId,
    // A name the server has not seen yet; once it has, the id above is what travels.
    newTopic = newTopicName?.takeIf { topicId == null },
    takenFrom = takenFrom,
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

private fun BasedOnDto.toDomain() = BasedOn(id = id, title = title, authorName = authorName)

private fun RankingDto.toDomain() = Ranking(
    uid = uid,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    arrangement = arrangement,
    tierLabels = tierLabels,
    updatedAtMillis = updatedAtMs,
    mine = mine,
)

/**
 * All nine, in the app's order, whatever the server sent: an id it does not
 * know is dropped, and a section it left out stands with nothing counted.
 */
internal fun sectionsInOrder(answer: List<SectionDto>): List<SectionStats> {
    val byId = answer.associateBy { it.id }
    return ListCategory.entries.map { section ->
        val stats = byId[section.id]
        SectionStats(
            section = section,
            listCount = stats?.listCount ?: 0,
            followerCount = stats?.followerCount ?: 0,
            newThisWeek = stats?.newThisWeek ?: 0,
            following = stats?.following ?: false,
            collage = stats?.collage.orEmpty().filter { it.startsWith("https://") }.take(COLLAGE_SIZE),
        )
    }
}

private const val COLLAGE_SIZE = 4

private fun SectionStats.followedBy(now: Boolean): SectionStats =
    if (following == now) this else copy(following = now, followerCount = (followerCount + if (now) 1 else -1).coerceAtLeast(0))

private fun FollowedSectionDto.toDomain(): FollowedSection? =
    ListCategory.fromId(id)?.let { FollowedSection(section = it, followerCount = followerCount) }

/** The way back from a kept summary to what the server would have sent, so one codec reads both. */
internal fun PublishedListSummary.toDto() = PublishedListSummaryDto(
    id = id,
    title = title,
    authorUid = authorUid,
    authorName = authorName,
    authorPhotoUrl = authorPhotoUrl,
    category = category.id,
    itemCount = itemCount,
    coverImageUrl = coverImageUrl,
    previewImages = previewImages,
    tierColors = tierColors,
    updatedAt = updatedAtMillis,
    takeCount = takeCount,
    anonymous = anonymous,
    topicId = topicId,
    topicName = topicName,
    rankingCount = rankingCount,
    basedOn = basedOn?.let { BasedOnDto(id = it.id, title = it.title, authorName = it.authorName) },
)
