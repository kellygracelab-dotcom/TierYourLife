package com.artiuillab.tieryourlife.feature.community.data.remote.dto

import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Which part of a cover, a card's or a topic's or section's picture shows; absent for the picture's default. */
@Serializable
data class CoverFocusDto(val x: Float, val y: Float, val zoom: Float)

/** Nothing out of range is drawn from. */
internal fun CoverFocusDto?.toDomain(): CoverFocus? = this?.let { CoverFocus(it.x, it.y, it.zoom).inRange() }

internal fun CoverFocus?.toDto(): CoverFocusDto? = this?.inRange()?.let { CoverFocusDto(it.x, it.y, it.zoom) }

/** A card's focus as a publish sends it: JsonNull for the picture's default, so the list lets go of one it had. */
internal fun CoverFocus?.toJson(): JsonElement = toDto()?.let {
    buildJsonObject {
        put("x", it.x)
        put("y", it.y)
        put("zoom", it.zoom)
    }
} ?: JsonNull

/** A card's focus as a list comes back with it; anything that does not read as three numbers is none. */
internal fun JsonElement?.toFocus(): CoverFocus? {
    val focus = this as? JsonObject ?: return null
    val (x, y, zoom) = listOf("x", "y", "zoom").map { key -> runCatching { focus[key]?.jsonPrimitive?.floatOrNull }.getOrNull() ?: return null }
    return CoverFocus(x, y, zoom).inRange()
}
