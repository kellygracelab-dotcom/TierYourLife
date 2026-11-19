package com.artiuillab.tieryourlife.core.settings

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The colour the app is drawn in, as this device keeps it: chosen here, or taken from the account. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ColourChoiceTest {

    private val preferences = SharedPreferencesAppPreferences(ApplicationProvider.getApplicationContext())

    @Test
    fun aChoice_isKept_andHeardBySync_withTheTimeItWasMade() = runBlocking {
        val choice = ColourChoice(preferences) { 1_000L }
        val heard = async(start = CoroutineStart.UNDISPATCHED) { choice.chosen.first() }

        choice.choose("matcha")

        assertEquals(ColourSetting("matcha", null, 1_000L), heard.await())
        // No own colour yet: none is made up for it (CONTRACT.md: hue null when they never made one).
        assertEquals(ColourSetting("matcha", null, 1_000L), preferences.colourSetting())
        assertEquals(preferences.colourSetting(), ColourChoice(preferences).current.value)
    }

    @Test
    fun anOwnHue_isKeptThroughTheReadyColours() {
        val choice = ColourChoice(preferences) { 1L }
        choice.choose("custom", 300)
        choice.choose("sky")

        assertEquals(300, choice.lastHue)
        assertEquals(ColourSetting("sky", 300, 1L), choice.current.value)
    }

    @Test
    fun theAccountsColour_isShownAndKept_butNotSentBack() = runBlocking {
        val choice = ColourChoice(preferences) { 1L }
        val heard = async(start = CoroutineStart.UNDISPATCHED) { choice.chosen.first() }

        choice.adopt(ColourSetting("rose", 10, 500))

        assertEquals(ColourSetting("rose", 10, 500), preferences.colourSetting())
        assertEquals(false, heard.isCompleted)
        heard.cancel()
    }

    @Test
    fun nothingChosen_isLavender_andOwnColourStartsFromIt() {
        val choice = ColourChoice(FakeAppPreferences()) { 1L }

        assertNull(choice.current.value)
        assertEquals(ColourChoice.LAVENDER_HUE, choice.lastHue)
    }
}
