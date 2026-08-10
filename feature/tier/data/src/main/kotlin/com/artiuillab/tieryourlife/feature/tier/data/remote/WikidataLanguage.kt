package com.artiuillab.tieryourlife.feature.tier.data.remote

private const val FALLBACK_LANGUAGE = "en"

internal fun wikidataLanguageCode(languageTag: String?): String {
    val tag = languageTag?.trim()?.lowercase().orEmpty()
    if (tag.isEmpty() || tag == "und") {
        return FALLBACK_LANGUAGE
    }

    val subtags = tag.split('-').filter { it.isNotEmpty() }
    return when {
        subtags.isEmpty() -> FALLBACK_LANGUAGE
        subtags.size > 2 -> subtags.first()
        else -> subtags.joinToString("-")
    }
}
