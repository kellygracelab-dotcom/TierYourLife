package com.artiuillab.tieryourlife.feature.community.data.remote

import retrofit2.HttpException

private const val WEB_PICTURES_DAILY_LIMIT = "WEB_PICTURES_DAILY_LIMIT"
private const val OUTSIDE_PICTURES = "pictures from other sites"
private const val HTTP_TOO_LARGE = 413
private const val HTTP_TOO_MANY_REQUESTS = 429

/** More pictures from other sites than the proxy fetches for one account in a day. */
fun Throwable.isWebPicturesToday(): Boolean =
    this is HttpException && code() == HTTP_TOO_MANY_REQUESTS && proxyCode() == WEB_PICTURES_DAILY_LIMIT

/** More pictures from other sites than one list may have. */
fun Throwable.isTooManyWebPictures(): Boolean =
    this is HttpException && code() == HTTP_TOO_LARGE && proxyError()?.contains(OUTSIDE_PICTURES, ignoreCase = true) == true
