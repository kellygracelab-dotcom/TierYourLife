package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.mapper.toDetailsByQid
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toDomain
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.IgdbApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.TmdbApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.WikidataSparqlApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.wikidataDetailsQuery
import com.artiuillab.tieryourlife.feature.tier.data.remote.wikidataLanguageCode
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
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

private const val MAX_DETAIL_IDS = 50

private const val FIRST_PAGE = 1

// TMDB answers with an error past its five hundredth page, whatever total_pages says.
private const val LAST_TMDB_PAGE = 500

class CatalogueSearchRepositoryImpl @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val wikidataApi: WikidataApi,
    private val wikidataSparqlApi: WikidataSparqlApi,
    private val igdbApi: IgdbApi,
) : CatalogueSearchRepository {

    override suspend fun search(
        query: String,
        languageTag: String?,
        page: Int,
    ): Result<CatalogueSearchPage> {
        val normalizedQuery = query.trim()

        if (normalizedQuery.isEmpty()) {
            return Result.success(CatalogueSearchPage(items = emptyList(), hasMore = false))
        }

        val resolvedLanguage = languageTag ?: Locale.getDefault().toLanguageTag()

        // Wikidata hands over everything it has on the first ask, so a later
        // page is TMDB alone rather than the same candidates a second time.
        if (page > FIRST_PAGE) {
            return fetchTmdb(normalizedQuery, resolvedLanguage, page).map { tmdb ->
                CatalogueSearchPage(
                    items = CatalogueSearchMerger.rank(normalizedQuery, tmdb.items),
                    hasMore = tmdb.hasMore,
                )
            }
        }

        return coroutineScope {
            val tmdbDeferred = async { fetchTmdb(normalizedQuery, resolvedLanguage, page) }
            val wikidataDeferred = async { fetchWikidata(normalizedQuery, resolvedLanguage) }
            val gamesDeferred = async { fetchGames(normalizedQuery) }

            val tmdb = tmdbDeferred.await()

            CatalogueSearchMerger.merge(
                query = normalizedQuery,
                tmdbResult = tmdb.map { it.items },
                wikidataResult = wikidataDeferred.await(),
                gamesResult = gamesDeferred.await(),
            ).map { items ->
                CatalogueSearchPage(items = items, hasMore = tmdb.getOrNull()?.hasMore == true)
            }
        }
    }

    private suspend fun fetchTmdb(query: String, language: String, page: Int): Result<TmdbPage> {
        val timedOut = withTimeoutOrNull(SEARCH_TIMEOUT_MILLIS) {
            try {
                val response = tmdbApi.searchMovies(query = query, language = language, page = page)
                Result.success(
                    TmdbPage(
                        items = response.results.map { it.toDomain() },
                        hasMore = response.page < minOf(response.totalPages, LAST_TMDB_PAGE),
                    ),
                )
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

    /**
     * Games, which no other catalogue here can show: a cover belongs to
     * whoever published it, so the free encyclopaedias have none. Given its
     * own timeout like the others, so a slow answer costs the search nothing
     * more than the games.
     */
    private suspend fun fetchGames(query: String): Result<List<CatalogueItem>> {
        val timedOut = withTimeoutOrNull(SEARCH_TIMEOUT_MILLIS) {
            try {
                Result.success(igdbApi.searchGames(query).results.map { it.toDomain() })
            } catch (exception: IOException) {
                Result.failure(exception)
            } catch (exception: HttpException) {
                Result.failure(exception)
            }
        }
        return timedOut ?: Result.failure(TimeoutException("Games search timed out"))
    }

    private suspend fun fetchWikidataCandidates(query: String, language: String): List<WikidataCandidate> {
        val wikidataLanguage = wikidataLanguageCode(language)
        val searchItems = wikidataApi.searchEntities(
            search = query,
            language = wikidataLanguage,
            uselang = wikidataLanguage,
        ).search

        if (searchItems.isEmpty()) {
            return emptyList()
        }

        val ids = searchItems.map { it.id }.take(MAX_DETAIL_IDS)
        val detailsByQid = wikidataSparqlApi.query(wikidataDetailsQuery(ids)).toDetailsByQid()

        return searchItems.map { item -> item.toDomain(detailsByQid[item.id]) }
    }
}

private data class TmdbPage(
    val items: List<CatalogueItem>,
    val hasMore: Boolean,
)
