package com.artiuillab.tieryourlife.feature.tier.presentation.common

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.HiddenEntry
import com.artiuillab.tieryourlife.core.settings.ThemeChoice

internal class FakeAppPreferences(
    private var backUpBoards: Boolean = true,
    private var offerAnswered: Boolean = false,
) : AppPreferences {
    private val hiddenLists = mutableMapOf<String, String>()
    private val hiddenAuthors = mutableMapOf<String, String>()

    override fun themeChoice(): ThemeChoice = ThemeChoice.SYSTEM
    override fun setThemeChoice(choice: ThemeChoice) = Unit
    override fun languageTag(): String? = null
    override fun setLanguageTag(tag: String?) = Unit
    override fun lastKnownCredits(): Int? = null
    override fun setLastKnownCredits(credits: Int?) = Unit
    override fun hiddenListIds(): Set<String> = hiddenLists.keys
    override fun hiddenLists(): List<HiddenEntry> = hiddenLists.map { HiddenEntry(it.key, it.value) }
    override fun hideList(publishedId: String, title: String) {
        hiddenLists[publishedId] = title
    }

    override fun unhideList(publishedId: String) {
        hiddenLists -= publishedId
    }

    override fun hiddenAuthorUids(): Set<String> = hiddenAuthors.keys
    override fun hiddenAuthors(): List<HiddenEntry> = hiddenAuthors.map { HiddenEntry(it.key, it.value) }
    override fun hideAuthor(authorUid: String, name: String) {
        hiddenAuthors[authorUid] = name
    }

    override fun unhideAuthor(authorUid: String) {
        hiddenAuthors -= authorUid
    }

    override fun backUpBoards(): Boolean = backUpBoards

    override fun setBackUpBoards(backUp: Boolean) {
        backUpBoards = backUp
    }

    override fun signInOfferAnswered(): Boolean = offerAnswered

    override fun markSignInOfferAnswered() {
        offerAnswered = true
    }

    override fun picturesOnWifiOnly(): Boolean = true

    override fun setPicturesOnWifiOnly(wifiOnly: Boolean) = Unit

    override fun lastSyncedAtMs(): Long? = null

    override fun setLastSyncedAtMs(atMs: Long?) = Unit

    override fun conflictsSeen(): Set<String> = emptySet()

    override fun markConflictSeen(listUid: String) = Unit
}
