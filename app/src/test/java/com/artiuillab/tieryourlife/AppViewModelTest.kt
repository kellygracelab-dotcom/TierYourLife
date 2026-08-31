package com.artiuillab.tieryourlife

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.HiddenEntry
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import org.junit.Assert.assertEquals
import org.junit.Test

class AppViewModelTest {

    @Test
    fun initialState_isWhateverWasStored_notADefault() {
        val preferences = FakeAppPreferences(themeChoice = ThemeChoice.DARK, languageTag = "uk")

        val state = AppViewModel(preferences).state.value

        assertEquals(ThemeChoice.DARK, state.themeChoice)
        assertEquals("uk", state.languageTag)
    }

    @Test
    fun changingTheTheme_updatesStateAndPersistsInOneStep() {
        val preferences = FakeAppPreferences()
        val viewModel = AppViewModel(preferences)

        viewModel.setThemeChoice(ThemeChoice.LIGHT)

        assertEquals(ThemeChoice.LIGHT, viewModel.state.value.themeChoice)
        assertEquals(listOf(ThemeChoice.LIGHT), preferences.storedThemeChoices)
    }

    @Test
    fun changingTheLanguage_updatesStateAndPersistsInOneStep() {
        val preferences = FakeAppPreferences()
        val viewModel = AppViewModel(preferences)

        viewModel.setLanguageTag("ar")

        assertEquals("ar", viewModel.state.value.languageTag)
        assertEquals(listOf("ar"), preferences.storedLanguageTags)
    }

    @Test
    fun choosingTheSystemLanguage_storesNull_ratherThanSkippingTheWrite() {
        val preferences = FakeAppPreferences(languageTag = "ja")
        val viewModel = AppViewModel(preferences)

        viewModel.setLanguageTag(null)

        assertEquals(null, viewModel.state.value.languageTag)
        assertEquals(listOf(null), preferences.storedLanguageTags)
    }

    @Test
    fun theTwoSettings_areIndependent() {
        val preferences = FakeAppPreferences()
        val viewModel = AppViewModel(preferences)

        viewModel.setThemeChoice(ThemeChoice.DARK)
        viewModel.setLanguageTag("pl")

        assertEquals(ThemeChoice.DARK, viewModel.state.value.themeChoice)
        assertEquals("pl", viewModel.state.value.languageTag)
    }
}

private class FakeAppPreferences(
    private var themeChoice: ThemeChoice = ThemeChoice.SYSTEM,
    private var languageTag: String? = null,
) : AppPreferences {
    override fun hiddenListIds(): Set<String> = emptySet()

    override fun hiddenLists(): List<HiddenEntry> = emptyList()

    override fun hideList(publishedId: String, title: String) = Unit

    override fun unhideList(publishedId: String) = Unit

    override fun hiddenAuthorUids(): Set<String> = emptySet()

    override fun hiddenAuthors(): List<HiddenEntry> = emptyList()

    override fun hideAuthor(authorUid: String, name: String) = Unit

    override fun unhideAuthor(authorUid: String) = Unit

    private var asPictures = false

    override fun boardsAsPictures(): Boolean = asPictures

    override fun setBoardsAsPictures(asPictures: Boolean) {
        this.asPictures = asPictures
    }

    override fun backUpBoards(): Boolean = true

    override fun setBackUpBoards(backUp: Boolean) = Unit

    override fun signInOfferAnswered(): Boolean = false

    override fun markSignInOfferAnswered() = Unit

    override fun picturesOnWifiOnly(): Boolean = true

    override fun setPicturesOnWifiOnly(wifiOnly: Boolean) = Unit

    override fun lastSyncedAtMs(): Long? = null

    override fun setLastSyncedAtMs(atMs: Long?) = Unit

    override fun conflictsSeen(): Set<String> = emptySet()

    override fun markConflictSeen(listUid: String) = Unit

    override fun lastKnownCredits(): Int? = null

    override fun setLastKnownCredits(credits: Int?) = Unit

    val storedThemeChoices = mutableListOf<ThemeChoice>()
    val storedLanguageTags = mutableListOf<String?>()

    override fun themeChoice(): ThemeChoice = themeChoice

    override fun setThemeChoice(choice: ThemeChoice) {
        themeChoice = choice
        storedThemeChoices += choice
    }

    override fun languageTag(): String? = languageTag

    override fun setLanguageTag(tag: String?) {
        languageTag = tag
        storedLanguageTags += tag
    }
}
