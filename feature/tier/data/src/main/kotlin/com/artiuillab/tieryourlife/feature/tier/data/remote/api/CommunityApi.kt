package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishListRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedFeedDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedIdDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedListDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CommunityApi {

    @GET("lists")
    suspend fun feed(
        @Query("category") category: String? = null,
        @Query("q") query: String? = null,
        @Query("author") author: String? = null,
    ): PublishedFeedDto

    @GET("lists/{id}")
    suspend fun open(@Path("id") id: String): PublishedListDto

    @POST("lists")
    suspend fun publish(@Body request: PublishListRequestDto): PublishedIdDto

    @POST("lists/{id}")
    suspend fun republish(@Path("id") id: String, @Body request: PublishListRequestDto): PublishedIdDto

    @DELETE("lists/{id}")
    suspend fun unpublish(@Path("id") id: String)

    @PATCH("lists")
    suspend fun refreshAuthor()
}
