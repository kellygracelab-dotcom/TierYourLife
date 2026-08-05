package com.artiuillab.tieryourlife.feature.tier.data.remote

private const val PROPERTY_IMAGE = "P18"
private const val PROPERTY_TMDB_MOVIE_ID = "P4947"

// Q-ids and nothing else. These come back from Wikidata's own search, so this is not defending
// against a hostile value so much as against a malformed one silently breaking the query for
// every other id in the same batch — one bad token and the whole request fails, taking twenty
// good results with it.
private val QID = Regex("^Q\\d+$")

// Asks the Query Service for exactly the two fields this app reads, for a whole batch of items
// at once.
//
// This replaced action=wbgetentities&props=claims, which has no way to ask for specific
// properties: it answers with every claim an item has. Measured on one search for "медведь",
// twenty items came to 428 KB — on every debounced keystroke — to extract two fields, and
// parsing all that is what crashed the app, because datavalue.value is a plain string for an
// image but a nested object for a property like "instance of". The same information through
// SPARQL is about 1.5 KB.
//
// OPTIONAL on both, so an item with no image and no TMDB id still comes back as a row rather
// than dropping out of the result set.
internal fun wikidataDetailsQuery(ids: List<String>): String {
    val values = ids.filter { QID.matches(it) }.joinToString(" ") { "wd:$it" }
    return "SELECT ?item ?image ?tmdb WHERE { " +
        "VALUES ?item { $values } " +
        "OPTIONAL { ?item wdt:$PROPERTY_IMAGE ?image } " +
        "OPTIONAL { ?item wdt:$PROPERTY_TMDB_MOVIE_ID ?tmdb } " +
        "}"
}

internal fun qidFromEntityUri(uri: String): String = uri.substringAfterLast('/')

// The Query Service hands back a ready Commons FilePath URL, already percent-encoded — spaces
// arrive as %20, which is the one thing hand-rolled encoding got wrong here before (URLEncoder
// produces "+", a literal plus inside a path).
//
// Two things still need doing to it. The scheme comes back as http, and this app has no
// cleartext-traffic permission, so Coil would refuse to load it. And a width turns a
// full-resolution original — some of these are many megabytes — into a thumbnail.
internal fun commonsThumbnailUrl(filePathUrl: String, width: Int = 500): String {
    val secure = if (filePathUrl.startsWith("http://")) {
        "https://" + filePathUrl.removePrefix("http://")
    } else {
        filePathUrl
    }
    val separator = if (secure.contains('?')) '&' else '?'
    return "$secure${separator}width=$width"
}
