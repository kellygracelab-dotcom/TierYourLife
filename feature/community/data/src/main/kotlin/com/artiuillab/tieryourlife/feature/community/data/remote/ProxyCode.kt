package com.artiuillab.tieryourlife.feature.community.data.remote

import com.artiuillab.tieryourlife.core.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.HttpException

@Serializable
private data class CodedRefusalDto(
    @SerialName("code") val code: String? = null,
)

/**
 * The proxy names every refusal in its body, since one status answers several
 * of them. Null when the failure is not an HTTP one, or the body says nothing.
 */
fun Throwable.proxyCode(): String? {
    val response = (this as? HttpException)?.response() ?: return null
    val body = response.errorBody()?.string().orEmpty()
    return runCatching { networkJson.decodeFromString<CodedRefusalDto>(body) }.getOrNull()?.code
}
