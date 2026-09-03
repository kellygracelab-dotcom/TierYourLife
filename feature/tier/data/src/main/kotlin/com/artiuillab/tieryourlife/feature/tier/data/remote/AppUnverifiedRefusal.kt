package com.artiuillab.tieryourlife.feature.tier.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/** The name the proxy gives the refusal it sends when Play will not vouch. */
private const val APP_UNVERIFIED = "APP_UNVERIFIED"

@Serializable
private data class RefusalDto(@SerialName("code") val code: String? = null)

/**
 * A 401 alone does not say which refusal it is: the same code answers a stale
 * ID token, which signing in again fixes. Only the code in the body separates
 * them, and a body that will not parse is read as the other kind -- telling
 * somebody their installation is unverified when it is not is the worse
 * mistake of the two, because there is nothing they can do about it.
 */
fun Throwable.isAppUnverified(): Boolean {
    val response = (this as? HttpException)?.response() ?: return false
    if (response.code() != HTTP_UNAUTHORIZED) return false
    val body = response.errorBody()?.string().orEmpty()
    return runCatching { networkJson.decodeFromString<RefusalDto>(body).code }.getOrNull() == APP_UNVERIFIED
}
