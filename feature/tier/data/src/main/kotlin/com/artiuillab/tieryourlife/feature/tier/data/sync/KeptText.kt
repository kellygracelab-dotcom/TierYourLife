package com.artiuillab.tieryourlife.feature.tier.data.sync

/** Text as `/boards` keeps it. Anything else comes back changed on the next pull. */
internal object KeptText {

    const val TITLE = 80
    const val CAPTION = 60
    const val CREDIT = 200

    /**
     * Stands in for a card without a name, which `/boards` refuses although
     * `/lists` takes one; the site shows these words for such a card too.
     */
    const val UNTITLED_CARD = "Untitled card"

    // JavaScript's \s, which the proxy collapses; Java's leaves out the Unicode spaces.
    private val whitespace = Regex("[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+")

    /** Runs of whitespace as one space, trimmed, cut to [maxLength]; null when nothing is left. */
    fun clean(value: String, maxLength: Int): String? =
        value.replace(whitespace, " ").trim(' ').takeIf { it.isNotEmpty() }?.take(maxLength)
}
