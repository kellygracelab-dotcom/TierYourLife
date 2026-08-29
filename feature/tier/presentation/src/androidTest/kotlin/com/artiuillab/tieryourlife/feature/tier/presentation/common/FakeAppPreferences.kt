package com.artiuillab.tieryourlife.feature.tier.presentation.common

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.ThemeChoice

internal class FakeAppPreferences : AppPreferences {
    private val hiddenLists = mutableSetOf<String>()
    private val hiddenAuthors = mutableSetOf<String>()

    override fun themeChoice(): ThemeChoice = ThemeChoice.SYSTEM
    override fun setThemeChoice(choice: ThemeChoice) = Unit
    override fun languageTag(): String? = null
    override fun setLanguageTag(tag: String?) = Unit
    override fun lastKnownCredits(): Int? = null
    override fun setLastKnownCredits(credits: Int?) = Unit
    override fun hiddenListIds(): Set<String> = hiddenLists
    override fun hideList(publishedId: String) {
        hiddenLists += publishedId
    }

    override fun hiddenAuthorUids(): Set<String> = hiddenAuthors
    override fun hideAuthor(authorUid: String) {
        hiddenAuthors += authorUid
    }
}
