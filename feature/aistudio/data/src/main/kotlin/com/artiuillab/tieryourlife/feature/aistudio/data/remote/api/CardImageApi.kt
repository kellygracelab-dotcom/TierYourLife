package com.artiuillab.tieryourlife.feature.aistudio.data.remote.api

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.CreditsResponseDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.GenerateImageRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CardImageApi {

    // The raw Response is what carries the refusal apart from the bytes: the
    // proxy answers 402 when the account has nothing left to spend.
    @POST("generate")
    suspend fun generate(
        @Body request: GenerateImageRequestDto,
    ): Response<ResponseBody>

    @GET("credits")
    suspend fun credits(): CreditsResponseDto
}
