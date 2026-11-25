package com.artiuillab.tieryourlife.core.network

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

/** A slow list's report says whether its call waited on a token, and the proxy is not five calls at a time. */
class SlowWaitTest {

    private val logged = mutableListOf<String>()
    private val tree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            logged += message
        }
    }
    private var clock = 0L

    @Before fun plant() = Timber.plant(tree)

    @After fun uproot() = Timber.uproot(tree)

    @Test
    fun aLongWait_leavesABreadcrumb() {
        val token = noteSlow("App Check token", { clock }) {
            clock += 2_400
            "token"
        }

        assertEquals("token", token)
        assertEquals(listOf("App Check token took 2400 ms"), logged)
    }

    @Test
    fun aShortWait_leavesNothing() {
        noteSlow("ID token", { clock }) { clock += 200 }

        assertTrue(logged.isEmpty())
    }

    @Test
    fun theProxyClient_takesMoreThanFiveCallsAtOnce() {
        val client = ProxyModule.provideProxyOkHttpClient(AppCheckInterceptor { null }, IdTokenInterceptor { null }, ErasedAccounts())

        assertEquals(PROXY_CALLS_AT_ONCE, client.dispatcher.maxRequestsPerHost)
    }
}
