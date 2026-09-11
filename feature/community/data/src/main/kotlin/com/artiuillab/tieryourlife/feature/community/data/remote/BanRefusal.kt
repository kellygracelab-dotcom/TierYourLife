package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.core.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_FORBIDDEN

private const val BANNED = "BANNED"

@Serializable
private data class BanNoticeDto(
    @SerialName("code") val code: String? = null,
    @SerialName("until") val until: Long? = null,
)

data class BanNotice(val untilMillis: Long?)

/**
 * The proxy refuses a banned author with a 403 that names the ban and when it
 * lifts. The same status answers somebody who is not signed in, so only the
 * code in the body makes it a ban; a body that will not parse is not one.
 */
fun Throwable.banNotice(): BanNotice? {
    val response = (this as? HttpException)?.response() ?: return null
    if (response.code() != HTTP_FORBIDDEN) return null
    val body = response.errorBody()?.string().orEmpty()
    val notice = runCatching { networkJson.decodeFromString<BanNoticeDto>(body) }.getOrNull() ?: return null
    return if (notice.code == BANNED) BanNotice(notice.until) else null
}
