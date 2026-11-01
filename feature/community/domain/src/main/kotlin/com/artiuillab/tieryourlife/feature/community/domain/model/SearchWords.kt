package com.artiuillab.tieryourlife.feature.community.domain.model

import java.text.Normalizer

/**
 * Words compared the way the server compares them (the proxy's src/search.ts):
 * lower case, accents and marks gone so ё is е and é is e, й kept.
 */
object SearchWords {
    private val marks = Regex("\\p{M}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    fun fold(text: String): String = buildString {
        for (char in text.lowercase()) {
            if (char == 'й') append(char) else append(Normalizer.normalize(char.toString(), Normalizer.Form.NFKD).replace(marks, ""))
        }
    }

    fun of(text: String): List<String> = fold(text).split(separators).filter { it.isNotEmpty() }

    /** Whether every typed word starts some word of [names]. */
    fun matches(typed: String, names: Iterable<String>): Boolean {
        val wanted = of(typed)
        if (wanted.isEmpty()) return false
        val held = names.flatMap(::of)
        return wanted.all { word -> held.any { it.startsWith(word) } }
    }
}

/** Whether what was typed names this section in any language, or by its id. */
fun Section.matchesSearch(typed: String, label: String = name): Boolean =
    SearchWords.matches(typed, listOf(label, name, id.replace('_', ' ')) + names.values)
