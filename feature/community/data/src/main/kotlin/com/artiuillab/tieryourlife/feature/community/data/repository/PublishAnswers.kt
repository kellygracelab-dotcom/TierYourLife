package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.network.ExpectedUserMismatch
import com.artiuillab.tieryourlife.feature.community.data.remote.banNotice
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishListRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedIdDto
import com.artiuillab.tieryourlife.feature.community.data.remote.isAppUnverified
import com.artiuillab.tieryourlife.feature.community.data.remote.isTooManyWebPictures
import com.artiuillab.tieryourlife.feature.community.data.remote.isWebPicturesToday
import com.artiuillab.tieryourlife.feature.community.data.remote.proxyCode
import com.artiuillab.tieryourlife.feature.community.data.remote.proxyDetail
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import retrofit2.HttpException
import java.io.IOException

internal fun Throwable.asPublishError(): PublishError = when {
    this is ExpectedUserMismatch -> PublishError.NotSignedIn
    isAppUnverified() -> PublishError.NotVerified
    this is HttpException -> when (code()) {
        HTTP_FORBIDDEN -> banNotice()?.let { PublishError.Banned(it.untilMillis) }
            // Replacing a list another account published: this board's link is not this person's.
            ?: if (proxyCode() == NOT_YOURS) PublishError.ListGone else PublishError.NotSignedIn
        HTTP_NOT_FOUND -> PublishError.ListGone
        HTTP_CONFLICT -> when (proxyCode()) {
            PUBLICATION_CHANGED -> PublishError.ChangedElsewhere
            "LIST_CHANGED" -> PublishError.SourceChanged
            TOO_MANY_PENDING_TOPICS -> PublishError.TooManyPendingTopics
            RANK_INSTEAD -> PublishError.RankInstead
            TOO_MANY_LISTS -> publicationQuotaRefusal()
            else -> PublishError.TooManyLists
        }
        HTTP_BAD_REQUEST -> when {
            proxyCode() == TOPIC_UNAVAILABLE -> PublishError.TopicUnavailable
            proxyDetail() == CATEGORY -> PublishError.SectionUnavailable
            else -> PublishError.Unknown
        }
        HTTP_TOO_LARGE -> if (isTooManyWebPictures()) PublishError.TooManyWebPictures else PublishError.TooLarge
        HTTP_TOO_MANY_REQUESTS -> if (isWebPicturesToday()) PublishError.WebPicturesToday else PublishError.Unknown
        HTTP_UNPROCESSABLE -> PublishError.PictureRefused
        else -> PublishError.Unknown
    }

    this is IOException -> PublishError.Offline
    else -> PublishError.Unknown
}

/**
 * "existing" writes nothing: the list out there is the board as an earlier,
 * unanswered publish sent it, so this board goes over it before the board
 * may call itself published.
 */
internal suspend fun sendOut(
    publishedId: String?,
    request: PublishListRequestDto,
    publish: suspend (PublishListRequestDto) -> PublishedIdDto,
    republish: suspend (String, PublishListRequestDto) -> PublishedIdDto,
): PublishedIdDto {
    val first = if (publishedId == null) publish(request) else republish(publishedId, request)
    return if (first.existing) republish(first.id, request) else first
}

internal fun Throwable.isCursorGone(): Boolean =
    this is HttpException && code() == HTTP_CONFLICT && proxyCode() == CURSOR_GONE

/**
 * The topic the answer names when it is not the board's: the list's own, a
 * moderator's move perhaps, which the board learns. A name the board sent
 * as new is the topic it became, and a ranking has no topic of its own.
 */
internal fun TierList.topicToLearn(answer: PublishedIdDto): String? =
    answer.topicId?.takeIf { !answer.ranking && it != topicId && newTopicName == null }

private const val TOO_MANY_PENDING_TOPICS = "TOO_MANY_PENDING_TOPICS"
private const val TOO_MANY_LISTS = "TOO_MANY_LISTS"
private const val RANK_INSTEAD = "RANK_INSTEAD"
private const val PUBLICATION_CHANGED = "PUBLICATION_CHANGED"
private const val CURSOR_GONE = "CURSOR_GONE"
private const val NOT_YOURS = "NOT_YOURS"
private const val CATEGORY = "category"
private const val TOPIC_UNAVAILABLE = "TOPIC_UNAVAILABLE"
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_TOO_LARGE = 413
private const val HTTP_UNPROCESSABLE = 422
private const val HTTP_TOO_MANY_REQUESTS = 429
