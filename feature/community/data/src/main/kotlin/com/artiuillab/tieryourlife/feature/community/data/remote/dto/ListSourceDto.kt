package com.artiuillab.tieryourlife.feature.community.data.remote.dto

import com.artiuillab.tieryourlife.feature.community.domain.model.ListSource
import kotlinx.serialization.Serializable
import java.net.URI

/** A house list's `listSource` (Proxy #141): only editorial lists carry one. */
@Serializable
data class ListSourceDto(val label: String = "", val url: String = "")

/** Held to the server's own limits, so a page never shows a line or opens an address the server would refuse. */
internal fun ListSourceDto.toDomain(): ListSource? {
    val label = label.trim().takeIf { it.isNotEmpty() && it.length <= MAX_LABEL && it.none { c -> c == '\n' || c == '\r' } }
    val url = url.trim().takeIf { it.length <= MAX_URL && it.startsWith("https://", ignoreCase = true) && it.hasHost() }
    return if (label != null && url != null) ListSource(label, url) else null
}

private fun String.hasHost(): Boolean = runCatching { !URI(this).host.isNullOrBlank() }.getOrDefault(false)

private const val MAX_LABEL = 120
private const val MAX_URL = 500
