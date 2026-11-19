package com.artiuillab.tieryourlife.feature.community.data.remote

import retrofit2.HttpException

private const val VISION_DAILY_LIMIT = "VISION_DAILY_LIMIT"
private const val REPORT_DAILY_LIMIT = "REPORT_DAILY_LIMIT"
private const val UNCHECKED = "unchecked"
private const val HTTP_TOO_MANY_REQUESTS = 429

/** A photograph SafeSearch could not judge, an animation among them (Proxy #173): a still one may go. */
fun Throwable.isPictureUnchecked(): Boolean = proxyCode() == "PICTURE_REFUSED" && proxyBecause() == UNCHECKED

/** More of one's own photographs than the server looks at for one account in a day. */
fun Throwable.isPicturesCheckedToday(): Boolean =
    this is HttpException && code() == HTTP_TOO_MANY_REQUESTS && proxyCode() == VISION_DAILY_LIMIT

/** More new reports than one person may send in a day. */
fun Throwable.isReportsToday(): Boolean =
    this is HttpException && code() == HTTP_TOO_MANY_REQUESTS && proxyCode() == REPORT_DAILY_LIMIT
