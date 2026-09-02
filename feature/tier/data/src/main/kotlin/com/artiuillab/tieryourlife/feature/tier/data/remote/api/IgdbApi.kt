package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.GameSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface IgdbApi {

    @GET("search")
    suspend fun searchGames(@Query("q") query: String): GameSearchResponseDto
}
