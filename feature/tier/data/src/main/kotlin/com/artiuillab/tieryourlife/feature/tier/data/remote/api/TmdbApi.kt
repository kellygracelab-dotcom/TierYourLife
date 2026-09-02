package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {

    /**
     * Films, series and people at once.
     *
     * One request rather than three: the same catalogue holds all of them,
     * and a search for a name should not have to be told in advance whether
     * it is a film, a programme or the person who made both.
     */
    @GET("3/search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("page") page: Int,
    ): MovieSearchResponseDto
}
