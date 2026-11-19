package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.LinkAnswerDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.LinkAskDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.UploadsAnswerDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Proxy CONTRACT.md, `POST /lists/pictures/link`. */
interface PictureLinksApi {

    @POST("lists/pictures/link")
    suspend fun link(@Body ask: LinkAskDto): LinkAnswerDto

    /** Proxy CONTRACT.md, `GET /lists/pictures/uploads[?count=n]`. */
    @GET("lists/pictures/uploads")
    suspend fun uploads(@Query("count") count: Int?): UploadsAnswerDto
}
