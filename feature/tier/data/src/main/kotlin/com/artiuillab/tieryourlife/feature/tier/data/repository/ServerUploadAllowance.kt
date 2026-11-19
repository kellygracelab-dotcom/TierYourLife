package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PictureLinksApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.UploadsRefusalDto
import com.artiuillab.tieryourlife.feature.tier.domain.repository.UploadAllowance
import com.artiuillab.tieryourlife.feature.tier.domain.repository.UploadRoom
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class ServerUploadAllowance internal constructor(
    private val api: PictureLinksApi,
    private val now: () -> Long,
) : UploadAllowance {

    @Inject
    constructor(api: PictureLinksApi) : this(api, System::currentTimeMillis)

    override suspend fun ask(count: Int?): UploadRoom = try {
        api.uploads(count?.coerceIn(1, MAX_ASKED))
            .uploads
            ?.let { UploadRoom.Left(it.left.coerceAtLeast(0), it.resetsAt) }
            ?: UploadRoom.Unknown
    } catch (e: CancellationException) {
        throw e
    } catch (refused: HttpException) {
        refusalOf(refused)
    } catch (e: IOException) {
        Timber.i(e, "How many pictures may go up today is not known now: no network")
        UploadRoom.Unknown
    } catch (e: SerializationException) {
        Timber.w(e, "Today's uploads came in a shape this app does not read")
        UploadRoom.Unknown
    }

    private fun refusalOf(refused: HttpException): UploadRoom {
        val body = runCatching {
            networkJson.decodeFromString<UploadsRefusalDto>(refused.response()?.errorBody()?.string().orEmpty())
        }.getOrNull()
        if (refused.code() != HTTP_TOO_MANY || body?.code !in FULL) {
            Timber.i("Asking about today's uploads was refused: HTTP %d %s", refused.code(), body?.code)
            return UploadRoom.Unknown
        }
        val retryAfter = refused.response()?.headers()?.get("Retry-After")?.trim()?.toLongOrNull()?.let { now() + it * MILLIS_PER_SECOND }
        val retryAt = body?.retryAt ?: body?.uploads?.resetsAt ?: retryAfter ?: (now() + FALLBACK_PAUSE_MILLIS)
        return UploadRoom.Full(retryAt, guest = body?.code == SIGN_IN_FOR_MORE)
    }

    private companion object {
        const val MAX_ASKED = 100
        const val HTTP_TOO_MANY = 429
        const val SIGN_IN_FOR_MORE = "SIGN_IN_FOR_MORE_UPLOADS"
        val FULL = setOf(SIGN_IN_FOR_MORE, "UPLOAD_DAILY_LIMIT")
        const val MILLIS_PER_SECOND = 1_000L
        const val FALLBACK_PAUSE_MILLIS = 60 * 60 * 1_000L
    }
}
