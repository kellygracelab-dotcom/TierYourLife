package com.artiuillab.tieryourlife.feature.account.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/** Proxy CONTRACT.md, boards, Settings: what the account chose for how the app looks. Only the caller's own. */
interface SettingsApi {
    @GET("boards/settings")
    suspend fun settings(): SettingsDto

    @PUT("boards/settings/colour")
    suspend fun keepColour(@Body colour: ColourWriteDto): ColourDto
}

@Serializable
data class SettingsDto(val colour: ColourDto? = null)

/** A ColourSetting; [landed] only on a write, false when the account kept a later choice and says which. */
@Serializable
data class ColourDto(
    val name: String = "",
    val hue: Int = 0,
    val changedAt: Long = 0,
    val updatedAt: Long = 0,
    val landed: Boolean = true,
)

@Serializable
data class ColourWriteDto(val name: String, val hue: Int, val changedAt: Long)
