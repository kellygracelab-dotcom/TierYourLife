package com.artiuillab.tieryourlife.core.usage

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface UsageApi {
    /** 204 counted; 400 refused as it is; 429 enough for today. */
    @POST("lists/stats")
    suspend fun send(@Body batch: UsageBatch): Response<Unit>
}
