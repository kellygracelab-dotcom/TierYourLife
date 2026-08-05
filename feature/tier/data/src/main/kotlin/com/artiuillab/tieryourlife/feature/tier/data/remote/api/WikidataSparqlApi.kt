package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSparqlResponseDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

// The Query Service lives on its own host (query.wikidata.org), so it gets its own Retrofit
// instance rather than sharing the Action API's — see NetworkModule.
//
// POST rather than GET: a batch of twenty ids makes the query long enough to be worth keeping
// out of a URL, and the service accepts form-encoded POST for exactly that reason. The Accept
// header is what selects the JSON results format over the default XML.
interface WikidataSparqlApi {

    @FormUrlEncoded
    @Headers("Accept: application/sparql-results+json")
    @POST("sparql")
    suspend fun query(
        @Field("query") query: String,
    ): WikidataSparqlResponseDto
}
