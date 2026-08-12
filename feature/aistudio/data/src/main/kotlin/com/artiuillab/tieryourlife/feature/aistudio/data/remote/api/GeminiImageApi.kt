package com.artiuillab.tieryourlife.feature.aistudio.data.remote.api

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.InteractionRequestDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.InteractionResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface GeminiImageApi {

    @POST("v1beta/interactions")
    suspend fun createInteraction(
        @Body request: InteractionRequestDto,
    ): InteractionResponseDto
}
