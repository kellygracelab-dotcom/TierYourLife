package com.artiuillab.tieryourlife.feature.tier.data.remote

import java.net.URLEncoder

private const val COMMONS_FILE_PATH_BASE = "https://commons.wikimedia.org/wiki/Special:FilePath/"

// URLEncoder.encode is built for application/x-www-form-urlencoded query strings, where a
// space becomes "+" — but this filename lands in a URL *path* segment, where "+" is a literal
// character, not a decoded space, and the request 404s. Encode with URLEncoder for everything
// else, then fix up the one character it gets wrong for this position.
fun commonsFilePathUrl(filename: String): String {
    val encoded = URLEncoder.encode(filename, "UTF-8").replace("+", "%20")
    return "$COMMONS_FILE_PATH_BASE$encoded?width=500"
}
