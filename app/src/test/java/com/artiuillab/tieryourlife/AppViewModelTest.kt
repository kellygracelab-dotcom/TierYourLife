package com.artiuillab.tieryourlife

import com.artiuillab.tieryourlife.feature.tier.domain.model.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.domain.repository.AppPreferences
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

    // "Default" in Settings arrives as null and must reach the store as null — the tag is
    // cleared only when the user asks for it, never on a plain launch. See MainActivity.
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
