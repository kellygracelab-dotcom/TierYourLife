package com.artiuillab.tieryourlife.feature.community.domain.model

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** What the house translates its lists into, as the server keys them; English is what they are written in. */
val TRANSLATED_LOCALES: Set<String> = setOf("ru", "uk", "de", "es", "fr", "ja", "pl", "pt-BR", "tr", "ar")

private const val BRAZIL = "br"

/**
 * The locale a house list is read in by an app drawn in [languageTag], or
 * null for its texts as written. Brazilian Portuguese is the only Portuguese
 * translated, so any other reads the source.
 */
fun translationLocale(languageTag: String): String? {
    val subtags = languageTag.trim().replace('_', '-').split('-').takeWhile { it.length > 1 }
    val language = subtags.firstOrNull()?.lowercase() ?: return null
    if (language == "pt") return "pt-BR".takeIf { subtags.drop(1).any { it.equals(BRAZIL, ignoreCase = true) } }
    return language.takeIf { it in TRANSLATED_LOCALES }
}

/**
 * A house list's texts in the readers' languages: per locale, each text as
 * written mapped to how it reads there. Empty on everybody else's lists.
 */
data class ListTranslations(val byLocale: Map<String, Map<String, String>> = emptyMap()) {

    /** Each text a reader in [languageTag] reads differently, keyed by how it is written. */
    fun wordsIn(languageTag: String): Map<String, String> = translationLocale(languageTag)?.let(byLocale::get).orEmpty()

    /** `translations[locale]?.[text] ?? text`. */
    fun t(languageTag: String, text: String): String = wordsIn(languageTag).textOf(text)
}

fun Map<String, String>.textOf(text: String): String = this[text]?.takeIf { it.isNotBlank() } ?: text

/** `names[locale] ?? title`. */
fun PublishedListSummary.titleIn(languageTag: String): String =
    translationLocale(languageTag)?.let(names::get)?.takeIf { it.isNotBlank() } ?: title

/** Its title, tiers and cards as [words] say them; the pool keeps its own name, which is the app's. */
fun TierList.readIn(words: Map<String, String>): TierList {
    if (words.isEmpty()) return this
    return copy(
        title = words.textOf(title),
        tiers = tiers.map { tier ->
            tier.copy(
                label = if (tier.isPool) tier.label else words.textOf(tier.label),
                caption = tier.caption?.let(words::textOf),
                items = tier.items.map { it.copy(title = words.textOf(it.title)) },
            )
        },
    )
}
