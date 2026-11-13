package com.artiuillab.tieryourlife.feature.tier.data.local.database

import androidx.room.TypeConverter
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PictureAttributionDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.toDomain
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.toDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import kotlinx.serialization.json.Json

/** A card's attribution in one column, as the JSON the server sends; a row that will not read is a card without one. */
class AttributionConverter {

    @TypeConverter
    fun fromStored(stored: String?): PictureAttribution? =
        stored?.let { runCatching { json.decodeFromString<PictureAttributionDto>(it).toDomain() }.getOrNull() }

    @TypeConverter
    fun toStored(attribution: PictureAttribution?): String? = attribution?.let { json.encodeToString(it.toDto()) }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
