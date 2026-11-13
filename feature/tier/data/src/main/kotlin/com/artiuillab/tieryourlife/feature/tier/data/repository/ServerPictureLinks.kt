package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PictureLinksApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.LinkAskDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.LinkRefusalDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.toDomain
import com.artiuillab.tieryourlife.feature.tier.data.sync.PictureSync
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PictureLinks
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * A picture the server kept comes down at once, so the card shows it now and
 * the sync never sends it back up.
 */
class ServerPictureLinks internal constructor(
    private val api: PictureLinksApi,
    private val accounts: AccountRepository,
    private val bringHome: suspend (account: String, pictureId: String) -> String?,
    private val now: () -> Long,
) : PictureLinks {

    @Inject
    constructor(api: PictureLinksApi, accounts: AccountRepository, pictures: PictureSync) :
        this(api, accounts, pictures::bringHome, System::currentTimeMillis)

    override suspend fun fetch(url: String): LinkPicture {
        val account = (accounts.current() as? Account.SignedIn)?.recordKey ?: return LinkPicture.SignInNeeded
        val answer = try {
            api.link(LinkAskDto(url.trim()))
        } catch (exception: IOException) {
            Timber.i(exception, "A picture from a link did not come: no network")
            return LinkPicture.Refused(LinkRefusal.Failed)
        } catch (exception: HttpException) {
            return refusalOf(exception)
        } catch (exception: SerializationException) {
            Timber.w(exception, "A picture from a link came in a shape this app does not read")
            return LinkPicture.Refused(LinkRefusal.Failed)
        }
        val attribution = answer.attribution?.toDomain()
        val credit = answer.credit?.trim()?.takeIf { it.isNotEmpty() }
        val pictureId = answer.pictureId?.takeIf { PICTURE_ID.matches(it) }
        val imageUrl = when {
            pictureId != null -> bringHome(account, pictureId)
            answer.imageUrl?.startsWith("https://") == true -> answer.imageUrl
            else -> null
        }
        return imageUrl?.let { LinkPicture.Found(it, attribution, credit) } ?: LinkPicture.Refused(LinkRefusal.Failed)
    }

    private fun refusalOf(refusal: HttpException): LinkPicture {
        val response = refusal.response()
        val body = runCatching { networkJson.decodeFromString<LinkRefusalDto>(response?.errorBody()?.string().orEmpty()) }.getOrNull()
        Timber.i("A picture from a link was refused: HTTP %d %s", refusal.code(), body?.code)
        return when {
            refusal.code() == HTTP_FORBIDDEN && body?.code == NOT_SIGNED_IN -> LinkPicture.SignInNeeded
            refusal.code() == HTTP_TOO_MANY -> {
                val retryAfter = response?.headers()?.get("Retry-After")?.trim()?.toLongOrNull()?.let { now() + it * MILLIS_PER_SECOND }
                LinkPicture.Limited(body?.retryAt ?: retryAfter ?: body?.quota?.resetsAt ?: (now() + FALLBACK_PAUSE_MILLIS))
            }
            else -> LinkPicture.Refused(
                when (body?.code) {
                    "NOT_A_PICTURE" -> LinkRefusal.NotAPicture
                    "TOO_BIG" -> LinkRefusal.TooBig
                    "PICTURE_REFUSED" -> LinkRefusal.Refused
                    "UNREACHABLE", "BAD_URL", "NOT_ALLOWED", "NO_URL" -> LinkRefusal.Unreachable
                    else -> LinkRefusal.Failed
                },
            )
        }
    }

    private companion object {
        val PICTURE_ID = Regex("[A-Za-z0-9_-]{1,100}")
        const val NOT_SIGNED_IN = "NOT_SIGNED_IN"
        const val HTTP_FORBIDDEN = 403
        const val HTTP_TOO_MANY = 429
        const val MILLIS_PER_SECOND = 1_000L
        const val FALLBACK_PAUSE_MILLIS = 60 * 60 * 1_000L
    }
}
