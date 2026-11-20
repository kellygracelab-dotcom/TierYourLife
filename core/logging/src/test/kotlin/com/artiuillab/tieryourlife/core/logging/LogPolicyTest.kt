package com.artiuillab.tieryourlife.core.logging

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException

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

    // Coordinator, 20.11: 676 non-fatals in 30 days, most of them a phone with no signal.
    @Test
    fun noConnectionIsWhatThePersonIsToldNotAFailureOfOurs() {
        listOf(
            UnknownHostException("Unable to resolve host"),
            SocketTimeoutException("Read timed out"),
            SocketException("Software caused connection abort"),
            ConnectException("Failed to connect"),
            ExecutionException(IOException("offline", UnknownHostException("example.invalid"))),
        ).forEach { error -> assertFalse(error.toString(), LogPolicy.isNonFatal(Log.WARN, error)) }
    }

    @Test
    fun theServerSayingNo_andOtherIoFailures_stillReport() {
        assertTrue(LogPolicy.isNonFatal(Log.WARN, IOException("HTTP 403")))
        assertTrue(LogPolicy.isNonFatal(Log.ERROR, ExecutionException(IllegalStateException("App attestation failed"))))
    }

    @Test
    fun aCauseThatPointsAtItselfEnds() {
        val loop = object : IllegalStateException("loop") {
            override val cause: Throwable get() = this
        }
        assertTrue(LogPolicy.isNonFatal(Log.WARN, loop))
    }
}
