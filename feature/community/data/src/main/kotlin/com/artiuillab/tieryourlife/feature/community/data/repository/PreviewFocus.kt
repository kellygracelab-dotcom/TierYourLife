package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.CoverFocusDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.toDomain
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus

/** Each preview picture with a focus, by its address; the first card with a picture speaks for it. */
internal fun previewFocusOf(images: List<String>, focus: List<CoverFocusDto?>?): Map<String, CoverFocus> = buildMap {
    images.zip(focus.orEmpty()).forEach { (image, dto) ->
        val url = image.webPictureOrNull() ?: return@forEach
        dto.toDomain()?.let { putIfAbsent(url, it) }
    }
}
