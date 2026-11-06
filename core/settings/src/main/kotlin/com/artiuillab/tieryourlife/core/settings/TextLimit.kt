package com.artiuillab.tieryourlife.core.settings

/** [take] that never leaves half of an emoji at the cut. */
fun String.takeWhole(n: Int): String {
    if (length <= n) return this
    val cut = if (n > 0 && this[n - 1].isHighSurrogate()) n - 1 else n
    return substring(0, cut)
}
