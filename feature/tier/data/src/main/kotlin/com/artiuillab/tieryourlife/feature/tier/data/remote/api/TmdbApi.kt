package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {

    @GET("3/search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("page") page: Int,
    ): MovieSearchResponseDto
}
