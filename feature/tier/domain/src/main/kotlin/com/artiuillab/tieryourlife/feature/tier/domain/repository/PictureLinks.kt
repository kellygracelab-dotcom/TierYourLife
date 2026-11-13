package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution

/** A card's picture by its address: the server fetches it, checks it and keeps it in the account's folder. */
interface PictureLinks {
    suspend fun fetch(url: String): LinkPicture
}

sealed interface LinkPicture {

    /** [imageUrl] is the phone's own copy, or a catalogue's address kept as it is. */
    data class Found(val imageUrl: String, val attribution: PictureAttribution?, val credit: String?) : LinkPicture

    data class Refused(val reason: LinkRefusal) : LinkPicture

    /** No more pictures from links until [until], in milliseconds since the epoch. */
    data class Limited(val until: Long) : LinkPicture

    data object SignInNeeded : LinkPicture
}

enum class LinkRefusal { NotAPicture, Unreachable, TooBig, Refused, Failed }

/** The first http or https address in [text], as somebody would paste or share it. */
fun linkIn(text: String): String? = text.trim().split(Regex("\\s+")).firstOrNull { word ->
    (word.startsWith("https://", ignoreCase = true) || word.startsWith("http://", ignoreCase = true)) && word.length > "https://".length
}

/** A page somebody names as where a picture is from: https, no login, no port, at most 500 characters. */
fun isPageAddress(text: String): Boolean {
    val page = text.trim()
    if (page.length > MAX_PAGE_LENGTH || !page.startsWith("https://", ignoreCase = true) || page.any { it.isWhitespace() }) return false
    val host = page.substring("https://".length).substringBefore('/').substringBefore('?').substringBefore('#')
    return host.isNotEmpty() && '@' !in host && ':' !in host && '.' in host
}

private const val MAX_PAGE_LENGTH = 500
