package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.mapper.toDomain
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.TmdbApi
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.repository.MovieSearchRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class MovieSearchRepositoryTmdbImpl @Inject constructor(
    private val api: TmdbApi,
) : MovieSearchRepository {

    override suspend fun searchMovies(
        query: String,
    ): Result<List<TierItem>> {
        val normalizedQuery = query.trim()

        if (normalizedQuery.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            val response = api.searchMovies(normalizedQuery)

            Result.success(
                response.results.map { movieDto ->
                    movieDto.toDomain()
                },
            )
        } catch (exception: IOException) {
            Result.failure(exception)
        } catch (exception: HttpException) {
            Result.failure(exception)
        }
    }
}