package com.artiuillab.tieryourlife.core.testing

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * One thread playing Android's main thread, so a view model runs on the JVM
 * the way it runs on a phone: one thing at a time, in real time.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {

    private lateinit var main: ExecutorCoroutineDispatcher

    override fun starting(description: Description) {
        main = newSingleThreadContext("test-main")
        Dispatchers.setMain(main)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
        main.close()
    }
}
