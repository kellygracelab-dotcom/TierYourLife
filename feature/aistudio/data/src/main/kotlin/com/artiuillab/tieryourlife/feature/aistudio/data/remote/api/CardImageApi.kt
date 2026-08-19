package com.artiuillab.tieryourlife.feature.aistudio.data.remote.api

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.GenerateImageRequestDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

interface CardImageApi {

    @POST("generate")
    suspend fun generate(
        @Body request: GenerateImageRequestDto,
    ): ResponseBody
}
