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
 * Writing answers with the whole response, not the body alone: a refused
 * write carries the board that was already there.
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

    /**
     * The account itself, not a board. "account" cannot collide with a board
     * uid, which is always Firestore's twenty characters.
     */
    @DELETE("boards/account")
    suspend fun eraseAccount()
}
