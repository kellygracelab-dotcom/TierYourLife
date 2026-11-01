package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PicturesDto
import retrofit2.http.GET
import retrofit2.http.Query

/** The proxy's picture search (src/pictureSearch.ts): every catalogue a scope names, and the web. */
interface PicturesApi {

    @GET("lists/pictures")
    suspend fun search(
        @Query("q") query: String,
        @Query("scope") scope: String,
        @Query("lang") language: String,
        @Query("page") page: Int,
        @Query("web") web: String? = null,
    ): PicturesDto
}
