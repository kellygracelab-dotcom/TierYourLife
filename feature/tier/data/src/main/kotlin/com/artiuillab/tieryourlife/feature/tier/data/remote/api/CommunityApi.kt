package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.FaceDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.ModerationQueueDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishListRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedFeedDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedIdDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.ReportRequestDto
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
        @Query("after") after: String? = null,
    ): PublishedFeedDto

    @GET("lists/mine")
    suspend fun myPublished(): PublishedFeedDto

    @GET("lists/reports")
    suspend fun reports(): ModerationQueueDto

    @POST("lists/{id}/takedown")
    suspend fun takeDown(@Path("id") id: String)

    @POST("lists/{id}/dismiss")
    suspend fun dismissReports(@Path("id") id: String)

    @GET("lists/{id}")
    suspend fun open(@Path("id") id: String): PublishedListDto

    @POST("lists")
    suspend fun publish(@Body request: PublishListRequestDto): PublishedIdDto

    @POST("lists/{id}")
    suspend fun republish(@Path("id") id: String, @Body request: PublishListRequestDto): PublishedIdDto

    @DELETE("lists/{id}")
    suspend fun unpublish(@Path("id") id: String)

    @POST("lists/face/{pictureId}")
    suspend fun makeFace(@Path("pictureId") pictureId: String): FaceDto

    @PATCH("lists")
    suspend fun refreshAuthor()

    @POST("lists/{id}/report")
    suspend fun report(@Path("id") id: String, @Body request: ReportRequestDto)
}
