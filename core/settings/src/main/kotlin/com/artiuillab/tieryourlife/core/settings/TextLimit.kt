package com.artiuillab.tieryourlife.core.settings

/** [take] that never cuts through an emoji, a flag or a letter and its accent. */
fun String.takeWhole(n: Int): String = if (length <= n) this else substring(0, wholeCutAtOrBefore(n))

/** The first emoji, flag or letter with its accents, whole. */
fun String.firstWhole(): String = substring(0, nextWholeCut(0))

/** The last place at or before [index] where cutting leaves every emoji, flag and accented letter whole. */
fun CharSequence.wholeCutAtOrBefore(index: Int): Int {
    var cut = index.coerceIn(0, length)
    while (splitsAt(cut)) cut--
    return cut
}

/** The first place after [index] where cutting leaves every emoji, flag and accented letter whole. */
fun CharSequence.nextWholeCut(index: Int): Int {
    var cut = index.coerceIn(0, length)
    if (cut == length) return cut
    do cut++ while (cut < length && splitsAt(cut))
    return cut
}

private const val ZERO_WIDTH_JOINER = 0x200D

private fun CharSequence.splitsAt(i: Int): Boolean {
    if (i <= 0 || i >= length) return false
    if (this[i - 1].isHighSurrogate() && this[i].isLowSurrogate()) return true
    val before = Character.codePointBefore(this, i)
    val after = Character.codePointAt(this, i)
    return before == ZERO_WIDTH_JOINER || after.joinsThePrevious() || insideAFlag(i, before, after)
}

private val VARIATION_SELECTORS = 0xFE00..0xFE0F
private val MORE_VARIATION_SELECTORS = 0xE0100..0xE01EF
private val SKIN_TONES = 0x1F3FB..0x1F3FF
private const val KEYCAP = 0x20E3
private val TAGS = 0xE0020..0xE007F
private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF
private val MARKS = setOf(Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK)

private fun Int.joinsThePrevious(): Boolean =
    this == ZERO_WIDTH_JOINER ||
        this in VARIATION_SELECTORS ||
        this in MORE_VARIATION_SELECTORS ||
        this in SKIN_TONES ||
        this == KEYCAP ||
        this in TAGS ||
        Character.getType(this).toByte() in MARKS

private fun Int.isRegionalIndicator() = this in REGIONAL_INDICATORS

// A flag is two regional indicators, so a run of them pairs up from its start.
private fun CharSequence.insideAFlag(i: Int, before: Int, after: Int): Boolean {
    if (!before.isRegionalIndicator() || !after.isRegionalIndicator()) return false
    var indicators = 0
    var j = i
    while (j > 0 && Character.codePointBefore(this, j).isRegionalIndicator()) {
        indicators++
        j -= 2
    }
    return indicators % 2 == 1
}
