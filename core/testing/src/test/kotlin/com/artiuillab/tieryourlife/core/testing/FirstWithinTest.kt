package com.artiuillab.tieryourlife.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class FirstWithinTest {

    @Test
    fun `the first matching value is returned`() {
        val found = runBlocking { flowOf(1, 2, 3).firstWithin { it > 1 } }

        assertEquals(2, found)
    }

    @Test
    fun `a value that never comes fails with the last one seen`() {
        val state = MutableStateFlow("loading")

        val error = assertThrows(AssertionError::class.java) {
            runBlocking { state.firstWithin(50.milliseconds) { it == "loaded" } }
        }

        assertTrue(error.message!!, error.message!!.contains("loading"))
    }

    @Test
    fun `a flow that ends without a match is not mistaken for a timeout`() {
        assertThrows(NoSuchElementException::class.java) {
            runBlocking { emptyFlow<Int>().firstWithin { true } }
        }
    }
}
