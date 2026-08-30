package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.presentation.common.FakeAppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiddenViewModelTest {

    @Test
    fun whatWasHidden_isListedByName() {
        val preferences = FakeAppPreferences()
        preferences.hideList("a", "Sci-fi films")
        preferences.hideAuthor("u1", "Olena M.")

        val state = HiddenViewModel(preferences).state.value

        assertEquals(listOf("Sci-fi films"), state.lists.map { it.label })
        assertEquals(listOf("Olena M."), state.people.map { it.label })
    }

    @Test
    fun showingAListAgain_takesItOffThisScreenAndOutOfTheHiddenSet() {
        val preferences = FakeAppPreferences()
        preferences.hideList("a", "Sci-fi films")
        val viewModel = HiddenViewModel(preferences)

        viewModel.showListAgain("a")

        assertTrue(viewModel.state.value.lists.isEmpty())
        assertTrue(preferences.hiddenListIds().isEmpty())
    }

    @Test
    fun showingAPersonAgain_leavesTheirListsAlone() {
        val preferences = FakeAppPreferences()
        preferences.hideList("a", "Sci-fi films")
        preferences.hideAuthor("u1", "Olena M.")
        val viewModel = HiddenViewModel(preferences)

        viewModel.showAuthorAgain("u1")

        assertTrue(viewModel.state.value.people.isEmpty())
        assertEquals(listOf("Sci-fi films"), viewModel.state.value.lists.map { it.label })
    }

    @Test
    fun nothingHidden_readsAsEmpty() {
        assertTrue(HiddenViewModel(FakeAppPreferences()).state.value.isEmpty)
    }
}
