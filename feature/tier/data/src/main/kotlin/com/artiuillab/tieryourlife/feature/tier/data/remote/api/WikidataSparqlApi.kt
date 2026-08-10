package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSparqlResponseDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface WikidataSparqlApi {

    @FormUrlEncoded
    @Headers("Accept: application/sparql-results+json")
    @POST("sparql")
    suspend fun query(
        @Field("query") query: String,
    ): WikidataSparqlResponseDto
}
