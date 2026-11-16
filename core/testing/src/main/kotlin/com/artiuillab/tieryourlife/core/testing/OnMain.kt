package com.artiuillab.tieryourlife.core.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * A view model's test run on the main thread, as its screen runs on a phone:
 * every call into the view model and every look at it happens between the
 * view model's own steps, never in the middle of one. From the test's own
 * thread a call can land halfway through what the view model is doing, which
 * no tap on a phone can, and the test then fails now and then with nothing
 * wrong in the code.
 *
 * Needs [MainDispatcherRule]. What the view model has launched runs only while
 * the test waits, so wait for it ([firstWithin]) before reading what it leaves.
 * Returns nothing whatever the last line is, so JUnit sees a void method.
 */
fun onMain(test: suspend CoroutineScope.() -> Unit) {
    runBlocking(Dispatchers.Main) { test() }
}
