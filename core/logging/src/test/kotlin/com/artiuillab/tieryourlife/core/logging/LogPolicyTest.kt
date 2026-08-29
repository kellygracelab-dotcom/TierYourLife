package com.artiuillab.tieryourlife.core.logging

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogPolicyTest {

    @Test
    fun debugChatterNeverLeavesThePhone() {
        assertFalse(LogPolicy.worthReporting(Log.VERBOSE))
        assertFalse(LogPolicy.worthReporting(Log.DEBUG))
        assertTrue(LogPolicy.worthReporting(Log.INFO))
    }

    // A swallowed exception is the whole point: the user saw a snackbar, we need
    // the stack.
    @Test
    fun aWarningCarryingAnExceptionIsANonFatal() {
        assertTrue(LogPolicy.isNonFatal(Log.WARN, IllegalStateException()))
        assertTrue(LogPolicy.isNonFatal(Log.ERROR, IllegalStateException()))
    }

    @Test
    fun aWarningWithoutAnExceptionIsOnlyABreadcrumb() {
        assertFalse(LogPolicy.isNonFatal(Log.WARN, null))
    }

    // Info is where the app narrates itself. Reporting those would bury the
    // failures under the story.
    @Test
    fun anInfoLineNeverReportsEvenWhenItCarriesAnException() {
        assertFalse(LogPolicy.isNonFatal(Log.INFO, IllegalStateException()))
    }

    @Test
    fun breadcrumbsKeepTheirTagWhenThereIsOne() {
        assertEquals("Account: Could not sign in", LogPolicy.breadcrumb("Account", "Could not sign in"))
        assertEquals("Could not sign in", LogPolicy.breadcrumb(null, "Could not sign in"))
        assertEquals("Could not sign in", LogPolicy.breadcrumb("", "Could not sign in"))
    }
}
