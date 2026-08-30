package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeepBoardRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardIndexDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardRevisionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The copy of someone's boards that outlives their phone.
 *
 * Writing answers with the whole response rather than the body alone: a
 * refused write carries the board that was already there, and that body is
 * the point of the refusal.
 */
interface BoardsApi {

    @GET("boards")
    suspend fun index(@Query("after") after: String? = null): KeptBoardIndexDto

    @GET("boards/{uid}")
    suspend fun board(@Path("uid") uid: String): KeptBoardDto

    @PUT("boards/{uid}")
    suspend fun keep(
        @Path("uid") uid: String,
        @Body request: KeepBoardRequestDto,
    ): Response<KeptBoardRevisionDto>

    @DELETE("boards/{uid}")
    suspend fun forget(@Path("uid") uid: String)
}
