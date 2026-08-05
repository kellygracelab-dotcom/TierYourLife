package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.mapper.toDomain
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.TmdbApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.wikidataLanguageCode
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataApi
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import com.artiuillab.tieryourlife.feature.tier.domain.search.CatalogueSearchMerger
import com.artiuillab.tieryourlife.feature.tier.domain.search.WikidataCandidate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeoutException
import javax.inject.Inject

private const val SEARCH_TIMEOUT_MILLIS = 5_000L

// Up to 50 pipe-separated ids per wbgetentities call; the search call itself is already
// capped at 20 results (WikidataApi.searchEntities' limit default), well under that ceiling.
private const val MAX_CLAIMS_IDS = 50

class CatalogueSearchRepositoryImpl @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val wikidataApi: WikidataApi,
) : CatalogueSearchRepository {

    override suspend fun search(
        query: String,
        languageTag: String?,
    ): Result<List<CatalogueItem>> {
        val normalizedQuery = query.trim()

        if (normalizedQuery.isEmpty()) {
            return Result.success(emptyList())
        }

        val resolvedLanguage = languageTag ?: Locale.getDefault().toLanguageTag()

        return coroutineScope {
            // Parallel, not sequential — a per-source timeout below means one slow source
            // can never hold up the other.
            val tmdbDeferred = async { fetchTmdb(normalizedQuery, resolvedLanguage) }
            val wikidataDeferred = async { fetchWikidata(normalizedQuery, resolvedLanguage) }

            CatalogueSearchMerger.merge(
                query = normalizedQuery,
                tmdbResult = tmdbDeferred.await(),
                wikidataResult = wikidataDeferred.await(),
            )
        }
    }

    private suspend fun fetchTmdb(query: String, language: String): Result<List<CatalogueItem>> {
        val timedOut = withTimeoutOrNull(SEARCH_TIMEOUT_MILLIS) {
            try {
                val response = tmdbApi.searchMovies(query = query, language = language)
                Result.success(response.results.map { it.toDomain() })
            } catch (exception: IOException) {
                Result.failure(exception)
            } catch (exception: HttpException) {
                Result.failure(exception)
            }
        }
        return timedOut ?: Result.failure(TimeoutException("TMDB search timed out"))
    }

    private suspend fun fetchWikidata(query: String, language: String): Result<List<WikidataCandidate>> {
        val timedOut = withTimeoutOrNull(SEARCH_TIMEOUT_MILLIS) {
            try {
                Result.success(fetchWikidataCandidates(query, language))
            } catch (exception: IOException) {
                Result.failure(exception)
            } catch (exception: HttpException) {
                Result.failure(exception)
            }
        }
        return timedOut ?: Result.failure(TimeoutException("Wikidata search timed out"))
    }

    private suspend fun fetchWikidataCandidates(query: String, language: String): List<WikidataCandidate> {
        // Wikidata's own language codes, not the BCP-47 tag Android hands us — it rejects
        // an unrecognised one outright rather than falling back, which would take the whole
        // source down silently. See wikidataLanguageCode.
        val wikidataLanguage = wikidataLanguageCode(language)
        val searchItems = wikidataApi.searchEntities(
            search = query,
            language = wikidataLanguage,
            uselang = wikidataLanguage,
        ).search

        if (searchItems.isEmpty()) {
            return emptyList()
        }

        val ids = searchItems.map { it.id }.take(MAX_CLAIMS_IDS)
        val entitiesById = wikidataApi.getEntities(ids = ids.joinToString("|")).entities

        return searchItems.map { item -> item.toDomain(entitiesById[item.id]) }
    }
}
