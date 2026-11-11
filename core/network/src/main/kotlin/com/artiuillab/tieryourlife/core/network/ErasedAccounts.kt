package com.artiuillab.tieryourlife.core.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

private const val ACCOUNT_ERASED = "ACCOUNT_ERASED"
private const val BEARER = "Bearer "
private const val REFUSAL_PEEK_BYTES = 4096L

/** Accounts the proxy said were erased, by uid: deleted from another phone while this one was still signed in. */
@Singleton
class ErasedAccounts @Inject constructor() {

    private val reported = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val uids: Flow<String> = reported

    fun report(uid: String) {
        reported.tryEmit(uid)
    }
}

/**
 * Every write is refused alike once the account is erased, whichever feature
 * sent it, so the refusal is heard here, once. The uid is the one the token
 * named: by the time the answer comes, somebody else may be signed in.
 */
class AccountErasedInterceptor(private val erased: (uid: String) -> Unit) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == HTTP_UNAUTHORIZED && response.refusalCode() == ACCOUNT_ERASED) {
            val uid = uidOfBearer(chain.request().header(AUTHORIZATION_HEADER))
            if (uid != null) erased(uid) else Timber.w("An erased account's refusal without a readable token")
        }
        return response
    }
}

@Serializable
private data class RefusalCodeDto(@SerialName("code") val code: String? = null)

@Serializable
private data class IdTokenClaimsDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("sub") val subject: String? = null,
)

private fun Response.refusalCode(): String? =
    runCatching { networkJson.decodeFromString<RefusalCodeDto>(peekBody(REFUSAL_PEEK_BYTES).string()).code }.getOrNull()

/** Identity hint for local actor binding/erasure dispatch, never authentication; the proxy verifies the token. */
internal fun uidOfBearer(header: String?): String? {
    val payload = header?.takeIf { it.startsWith(BEARER) }?.removePrefix(BEARER)?.split('.')?.getOrNull(1) ?: return null
    return runCatching {
        val claims = networkJson.decodeFromString<IdTokenClaimsDto>(String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8))
        (claims.userId ?: claims.subject)?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
