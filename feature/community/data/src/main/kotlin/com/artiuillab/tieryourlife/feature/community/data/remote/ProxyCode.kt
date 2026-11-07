package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.core.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_FORBIDDEN

private const val TARGET_IS_MODERATOR = "TARGET_IS_MODERATOR"

@Serializable
private data class CodedRefusalDto(
    @SerialName("code") val code: String? = null,
    @SerialName("detail") val detail: String? = null,
)

/**
 * The proxy names every refusal in its body, since one status answers several
 * of them. Null when the failure is not an HTTP one, or the body says nothing.
 */
fun Throwable.proxyCode(): String? = codedRefusal()?.code

/** A moderator acting on a list by staff, which only the owner may. */
fun Throwable.isTargetStaff(): Boolean =
    this is HttpException && code() == HTTP_FORBIDDEN && proxyCode() == TARGET_IS_MODERATOR

/** Which field a `400 INVALID` was about, such as "category". */
fun Throwable.proxyDetail(): String? = codedRefusal()?.detail

private fun Throwable.codedRefusal(): CodedRefusalDto? =
    runCatching { networkJson.decodeFromString<CodedRefusalDto>(errorText()) }.getOrNull()

/**
 * The body of a refusal, read without using it up: one failure is asked
 * about by more than one of these in turn.
 */
internal fun Throwable.errorText(): String {
    val body = (this as? HttpException)?.response()?.errorBody() ?: return ""
    return runCatching { body.source().peek().readUtf8() }.getOrDefault("")
}
