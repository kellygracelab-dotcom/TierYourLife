package com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AdoptGuestCreditsRequestDto(val guestToken: String)

/** [credits] is null when there was nothing to take over. */
@Serializable
data class AdoptedCreditsDto(val credits: Int? = null, val moved: Boolean = false)
