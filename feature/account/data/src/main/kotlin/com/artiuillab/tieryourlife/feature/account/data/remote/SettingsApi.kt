package com.artiuillab.tieryourlife.feature.account.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/** Proxy CONTRACT.md, boards, Settings: what the account chose for how the app looks. Only the caller's own. */
interface SettingsApi {
    @GET("boards/settings")
    suspend fun settings(): SettingsDto

    @PUT("boards/settings/colour")
    suspend fun keepColour(@Body colour: ColourWriteDto): ColourDto
}

/** Proxy CONTRACT.md, lists, "The account's photo": the slot read with [SettingsApi.settings] is changed here. */
interface FacePhotoApi {
    /** "Remove your photo": at once; the copy is kept a while for [restorePhoto]. */
    @DELETE("lists/face")
    suspend fun removePhoto(): RemovedPhotoDto

    @POST("lists/face/restore")
    suspend fun restorePhoto(): RestoredPhotoDto
}

@Serializable
data class SettingsDto(val colour: ColourDto? = null, val photo: PhotoSlotDto? = null)

/** The account's photo: the upload's id, its copy's address, when it was put there. */
@Serializable
data class PhotoSlotDto(val pictureId: String = "", val url: String = "", val setAt: Long = 0)

@Serializable
data class RemovedPhotoDto(val wasWorn: Boolean = false)

@Serializable
data class RestoredPhotoDto(val photo: PhotoSlotDto? = null, val wasWorn: Boolean = false)

/** A ColourSetting; [landed] only on a write, false when the account kept a later choice and says which. */
@Serializable
data class ColourDto(
    val name: String = "",
    val hue: Int? = null,
    val changedAt: Long = 0,
    val updatedAt: Long = 0,
    val landed: Boolean = true,
)

@Serializable
data class ColourWriteDto(val name: String, val hue: Int?, val changedAt: Long)
