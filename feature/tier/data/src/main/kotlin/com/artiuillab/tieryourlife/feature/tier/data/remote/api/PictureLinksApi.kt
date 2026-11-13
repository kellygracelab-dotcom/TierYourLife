package com.artiuillab.tieryourlife.feature.tier.data.remote.api

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.LinkAnswerDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.LinkAskDto
import retrofit2.http.Body
import retrofit2.http.POST

/** Proxy CONTRACT.md, `POST /lists/pictures/link`. */
interface PictureLinksApi {

    @POST("lists/pictures/link")
    suspend fun link(@Body ask: LinkAskDto): LinkAnswerDto
}
