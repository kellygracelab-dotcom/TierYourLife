package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataEntitiesResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

// Bound to a Retrofit instance whose baseUrl is https://www.wikidata.org/w/ (see NetworkModule)
// rather than absolute @Url paths — both calls hit the same single endpoint (api.php) with
// different action query params, which is exactly the shape a baseUrl + @GET path is for.
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

    // ids is up to 50 pipe-separated Q-ids, e.g. "Q1|Q2|Q3".
    @GET("api.php")
    suspend fun getEntities(
        @Query("ids") ids: String,
        @Query("action") action: String = "wbgetentities",
        @Query("props") props: String = "claims",
        @Query("format") format: String = "json",
    ): WikidataEntitiesResponseDto
}
