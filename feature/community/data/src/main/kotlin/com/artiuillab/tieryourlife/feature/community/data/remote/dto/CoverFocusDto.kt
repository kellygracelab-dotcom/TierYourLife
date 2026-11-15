package com.artiuillab.tieryourlife.feature.community.data.remote.dto

import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import kotlinx.serialization.Serializable

/** Which part of a cover or a topic's or section's picture shows; absent for the centre. */
@Serializable
data class CoverFocusDto(val x: Float, val y: Float, val zoom: Float)

/** Nothing out of range is drawn from, and the centre is no focus at all. */
internal fun CoverFocusDto?.toDomain(): CoverFocus? = this?.let { CoverFocus(it.x, it.y, it.zoom).inRange() }?.takeUnless { it.isCentred }

internal fun CoverFocus?.toDto(): CoverFocusDto? = this?.inRange()?.takeUnless { it.isCentred }?.let { CoverFocusDto(it.x, it.y, it.zoom) }
