package com.artiuillab.tieryourlife.feature.tier.data.remote

private const val PROPERTY_IMAGE = "P18"

/**
 * A logo, for the subjects that have one instead of a photograph. A footballer
 * has a portrait somebody took and gave away; their club has a badge, and that
 * is the only picture of a club there is.
 */
private const val PROPERTY_LOGO = "P154"
private const val PROPERTY_TMDB_MOVIE_ID = "P4947"

private val QID = Regex("^Q\\d+$")

internal fun wikidataDetailsQuery(ids: List<String>): String {
    val values = ids.filter { QID.matches(it) }.joinToString(" ") { "wd:$it" }
    return "SELECT ?item ?image ?logo ?tmdb WHERE { " +
        "VALUES ?item { $values } " +
        "OPTIONAL { ?item wdt:$PROPERTY_IMAGE ?image } " +
        "OPTIONAL { ?item wdt:$PROPERTY_LOGO ?logo } " +
        "OPTIONAL { ?item wdt:$PROPERTY_TMDB_MOVIE_ID ?tmdb } " +
        "}"
}

internal fun qidFromEntityUri(uri: String): String = uri.substringAfterLast('/')

internal fun commonsThumbnailUrl(filePathUrl: String, width: Int = 500): String {
    val secure = if (filePathUrl.startsWith("http://")) {
        "https://" + filePathUrl.removePrefix("http://")
    } else {
        filePathUrl
    }
    val separator = if (secure.contains('?')) '&' else '?'
    return "$secure${separator}width=$width"
}
