package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.ThemeChoice

// Not suspend and not a Flow: the domain module is plain Kotlin with no coroutines
// dependency, and the SharedPreferences-backed implementation is memory-cached after
// its first read, so a plain synchronous call is both correct and cheap.
interface AppPreferences {

    fun themeChoice(): ThemeChoice

    fun setThemeChoice(choice: ThemeChoice)

    // A BCP-47 language tag ("uk", "pt-BR", ...) or null to follow the system locale.
    // Stored here rather than through appcompat's own autoStoreLocales service: Google's
    // reference for AppLocalesMetadataHolderService notes that autoStoreLocales=true
    // performs a blocking read on the main thread and can trip StrictMode's disk-read
    // and disk-write policies, and this SharedPreferences-backed store is already
    // synchronous and memory-cached after its first read.
    fun languageTag(): String?

    fun setLanguageTag(tag: String?)
}
