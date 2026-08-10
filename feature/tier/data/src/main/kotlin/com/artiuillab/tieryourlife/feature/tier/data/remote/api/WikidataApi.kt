package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WikidataApi {

    @GET("api.php")
    suspend fun searchEntities(
        @Query("search") search: String,
        @Query("language") language: String,
        @Query("uselang") uselang: String,
        @Query("action") action: String = "wbsearchentities",
        @Query("type") type: String = "item",
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
    ): WikidataSearchResponseDto
}
