package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.settings.takeWhole

/** Text as `/boards` keeps it. Anything else comes back changed on the next pull. */
internal object KeptText {

    const val TITLE = 80
    const val CAPTION = 60
    const val CREDIT = 200
    const val URL = 2000

    /**
     * The name of a card with neither a name nor a picture the account can
     * keep, which `/boards` refuses; the site shows these words for such a
     * card too. A card with a kept picture goes up nameless.
     */
    const val UNTITLED_CARD = "Untitled card"

    // JavaScript's \s, which the proxy collapses; Java's leaves out the Unicode spaces.
    private val whitespace = Regex("[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+")

    /** Runs of whitespace as one space, trimmed, cut to [maxLength]; null when nothing is left. */
    fun clean(value: String, maxLength: Int): String? =
        value.replace(whitespace, " ").trim(' ').takeIf { it.isNotEmpty() }?.takeWhole(maxLength)

    /** A card's name as the account keeps it. */
    fun cardTitle(title: String, hasKeptPicture: Boolean): String =
        clean(title, TITLE) ?: if (hasKeptPicture) "" else UNTITLED_CARD

    /** A picture another device can fetch: one of ours by its id, or a web address the account keeps. */
    fun isKeptPicture(pictureId: String?, imageUrl: String?): Boolean =
        pictureId != null || imageUrl?.trim()?.let { it.startsWith("https://", ignoreCase = true) && it.length <= URL } == true
}
