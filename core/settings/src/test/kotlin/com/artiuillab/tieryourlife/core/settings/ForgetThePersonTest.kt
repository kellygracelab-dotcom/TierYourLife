package com.artiuillab.tieryourlife.core.settings

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A refused board is named by its title: the next person on the phone is not to read it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ForgetThePersonTest {

    @Test
    fun forgettingThePerson_forgetsEveryAccountsRefusedBoard() {
        val preferences = SharedPreferencesAppPreferences(ApplicationProvider.getApplicationContext())
        preferences.setBoardRefusal("uid-a", "Diary")
        preferences.setBoardRefusal("uid-b", "Films")

        preferences.forgetThePerson()

        assertNull(preferences.boardRefusal("uid-a"))
        assertNull(preferences.boardRefusal("uid-b"))
    }
}
