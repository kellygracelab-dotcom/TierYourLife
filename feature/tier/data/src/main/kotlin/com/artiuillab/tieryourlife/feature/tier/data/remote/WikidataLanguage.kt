package com.artiuillab.tieryourlife.feature.tier.data.remote

private const val FALLBACK_LANGUAGE = "en"

// Wikidata rejects a language it does not recognise outright — the request comes back with
// "badvalue" and no results — so an unusable tag does not degrade the search, it silences
// the whole source. Checked against the live API:
//
//   ru, uk, ja, pt, en   accepted
//   pt-BR, en-GB         rejected; pt-br, en-gb accepted
//   sr-Latn-RS           rejected in any case
//
// This is not an edge case. Android hands out region-qualified tags as a matter of course —
// the test device reports en-GB — so without this, Wikidata would have failed silently for
// most of the world while TMDB carried on answering, and nothing on screen would have said so.
//
// Hence: lower-case the tag, since that alone rescues every region variant; and where there
// are more than two subtags, fall back to the primary one, since Wikidata has no code of that
// shape.
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
