package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PicturesApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.SearchRefusalDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.toDomain
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchRefusal
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

/**
 * The server asks every catalogue a scope names, and the web, as it does for the site; the phone asks
 * nobody else. A person over the search limit is not sent to it again until the time it names; a busy
 * catalogue is said, so the screen can wait for it.
 */
class ServerCatalogueSearch internal constructor(
    private val api: PicturesApi,
    private val now: () -> Long,
    private val device: () -> Locale,
) : CatalogueSearchRepository {

    @Inject
    constructor(api: PicturesApi) : this(api, System::currentTimeMillis, Locale::getDefault)

    @Volatile
    private var limitedUntil = 0L

    override suspend fun search(
        query: String,
        languageTag: String?,
        page: Int,
        scope: PictureScope,
        everywhere: Boolean,
        quick: Boolean,
    ): Result<CatalogueSearchPage> {
        val typed = query.trim()
        if (typed.length < MIN_QUERY) return Result.success(CatalogueSearchPage(items = emptyList(), hasMore = false))
        if (now() < limitedUntil) return Result.failure(SearchRefusal.Limit(limitedUntil))
        return try {
            Result.success(
                api.search(
                    query = typed,
                    scope = (if (everywhere) PictureScope.All else scope).id,
                    language = searchLanguage(languageTag, device()),
                    page = page,
                    web = if (everywhere) "1" else null,
                    part = if (quick) "quick" else null,
                ).toDomain(),
            )
        } catch (exception: IOException) {
            Timber.i(exception, "The server's picture search failed on the network")
            Result.failure(exception)
        } catch (exception: HttpException) {
            val refusal = refusalOf(exception)
            if (refusal is SearchRefusal.Limit) limitedUntil = refusal.retryAt
            if (refusal == null) Timber.w(exception, "The server's picture search refused: HTTP ${exception.code()}")
            Result.failure(refusal ?: exception)
        } catch (exception: SerializationException) {
            Timber.w(exception, "The server's picture search answered in a shape this app does not read")
            Result.failure(exception)
        }
    }

    private fun refusalOf(refusal: HttpException): SearchRefusal? {
        val response = refusal.response() ?: return null
        if (refusal.code() != HTTP_TOO_MANY && refusal.code() != HTTP_UNAVAILABLE) return null
        val body = runCatching { networkJson.decodeFromString<SearchRefusalDto>(response.errorBody()?.string().orEmpty()) }.getOrNull()
        val retryAfter = response.headers()["Retry-After"]?.trim()?.toLongOrNull()?.let { now() + it * MILLIS_PER_SECOND }
        val retryAt = body?.retryAt ?: retryAfter
        return when (body?.code) {
            SEARCH_LIMIT -> SearchRefusal.Limit(retryAt ?: (now() + FALLBACK_PAUSE_MILLIS))
            SOURCE_BUSY -> SearchRefusal.Busy(retryAt)
            else -> null
        }
    }

    private companion object {
        const val MIN_QUERY = 2
        const val HTTP_TOO_MANY = 429
        const val HTTP_UNAVAILABLE = 503
        const val SEARCH_LIMIT = "SEARCH_LIMIT"
        const val SOURCE_BUSY = "SOURCE_BUSY"
        const val MILLIS_PER_SECOND = 1_000L
        const val FALLBACK_PAUSE_MILLIS = 60 * 60 * 1_000L
    }
}

/** The app's languages, as the site names them: one search in one language is one answer the server keeps for both. */
private val SITE_LANGUAGES = setOf("en", "uk", "ru", "es", "de", "fr", "pl", "tr", "ja", "ar")
private const val PORTUGUESE = "pt"
private const val BRAZILIAN = "pt-BR"
private const val ENGLISH = "en"

/**
 * The language the catalogues answer in: the one chosen in the app, or else the phone's, as the site's
 * own tag (`en`, never `en-US`); a language the app does not speak searches in English, as the app reads.
 */
internal fun searchLanguage(chosen: String?, device: Locale): String {
    val language = (chosen?.let(Locale::forLanguageTag) ?: device).language
    return when {
        language == PORTUGUESE -> BRAZILIAN
        language in SITE_LANGUAGES -> language
        else -> ENGLISH
    }
}
