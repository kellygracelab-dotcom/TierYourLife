package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.ThemeChoice

// Not suspend and not a Flow: the domain module is plain Kotlin with no coroutines
// dependency, and the SharedPreferences-backed implementation is memory-cached after
// its first read, so a plain synchronous call is both correct and cheap.
interface AppPreferences {

    fun themeChoice(): ThemeChoice

    fun setThemeChoice(choice: ThemeChoice)
}
