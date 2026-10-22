package com.artiuillab.tieryourlife.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.concurrent.Executors

/**
 * One thread playing Android's main thread, so a view model runs on the JVM
 * the way it runs on a phone: one thing at a time, in real time.
 *
 * The thread outlives each test on purpose. A view model's last coroutine can
 * still be finishing when its test returns; taking Main away under it made
 * that coroutine fail inside whichever test ran next.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(main)
    }

    private companion object {
        val main = Executors.newSingleThreadExecutor { task ->
            Thread(task, "test-main").apply { isDaemon = true }
        }.asCoroutineDispatcher()
    }
}
