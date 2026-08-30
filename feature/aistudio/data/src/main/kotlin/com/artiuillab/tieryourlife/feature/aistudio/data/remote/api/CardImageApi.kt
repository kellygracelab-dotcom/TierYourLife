package com.artiuillab.tieryourlife.feature.aistudio.data.remote.api

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.AdoptGuestCreditsRequestDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.AdoptedCreditsDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.CreditsResponseDto
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.GenerateImageRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CardImageApi {

    @POST("generate")
    suspend fun generate(
        @Body request: GenerateImageRequestDto,
    ): Response<ResponseBody>

    @GET("credits")
    suspend fun credits(): CreditsResponseDto

    /**
     * Both halves of the proof travel here: the caller's own token in the
     * header says where the credits are going, and the guest's says where they
     * are coming from. A uid in the body would let anyone drain anyone.
     */
    @POST("adoptGuestCredits")
    suspend fun adoptGuestCredits(@Body request: AdoptGuestCreditsRequestDto): AdoptedCreditsDto
}
