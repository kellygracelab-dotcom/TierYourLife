package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PicturesApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.SearchRefusalDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.toDomain
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

/**
 * The server asks every catalogue a scope names, and the web; when it cannot
 * be reached, the phone asks the two it always could ([direct]), scope or no
 * scope, so a search still finds films. A person over the server's search
 * limit is not sent to it again until the hour or day it names.
 */
class ServerCatalogueSearch internal constructor(
    private val api: PicturesApi,
    private val direct: CatalogueSearchRepositoryImpl,
    private val now: () -> Long,
) : CatalogueSearchRepository {

    @Inject
    constructor(api: PicturesApi, direct: CatalogueSearchRepositoryImpl) : this(api, direct, System::currentTimeMillis)

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
        if (typed.length < MIN_QUERY) return direct.search(typed, languageTag, page)
        val language = languageTag ?: Locale.getDefault().toLanguageTag()
        if (now() < limitedUntil) return directWhileLimited(typed, language, page)
        val asked = try {
            Result.success(
                api.search(
                    query = typed,
                    scope = (if (everywhere) PictureScope.All else scope).id,
                    language = language,
                    page = page,
                    web = if (everywhere) "1" else null,
                    part = if (quick) "quick" else null,
                ).toDomain(),
            )
        } catch (exception: IOException) {
            Timber.i(exception, "The server's picture search failed on the network; asking the catalogues directly")
            Result.failure(exception)
        } catch (exception: HttpException) {
            limitOf(exception)?.let { until ->
                Timber.i("Over the server's picture search limit until $until; asking the catalogues directly")
                limitedUntil = until
                return directWhileLimited(typed, language, page)
            }
            Timber.w(exception, "The server's picture search refused: HTTP ${exception.code()}; asking the catalogues directly")
            Result.failure(exception)
        } catch (exception: SerializationException) {
            Timber.w(exception, "The server's picture search answered in a shape this app does not read")
            Result.failure(exception)
        }
        return if (asked.isSuccess) asked else direct.search(typed, language, page)
    }

    private suspend fun directWhileLimited(query: String, language: String, page: Int) =
        direct.search(query, language, page).map { it.copy(notice = SearchNotice.SearchLimit(limitedUntil)) }

    private fun limitOf(refusal: HttpException): Long? {
        val response = refusal.response()
        if (refusal.code() != HTTP_TOO_MANY || response == null) return null
        val body = runCatching { networkJson.decodeFromString<SearchRefusalDto>(response.errorBody()?.string().orEmpty()) }.getOrNull()
        if (body?.code != SEARCH_LIMIT) return null
        val retryAfter = response.headers()["Retry-After"]?.trim()?.toLongOrNull()?.let { now() + it * MILLIS_PER_SECOND }
        return body.retryAt ?: retryAfter ?: (now() + FALLBACK_PAUSE_MILLIS)
    }

    private companion object {
        const val MIN_QUERY = 2
        const val HTTP_TOO_MANY = 429
        const val SEARCH_LIMIT = "SEARCH_LIMIT"
        const val MILLIS_PER_SECOND = 1_000L
        const val FALLBACK_PAUSE_MILLIS = 60 * 60 * 1_000L
    }
}
