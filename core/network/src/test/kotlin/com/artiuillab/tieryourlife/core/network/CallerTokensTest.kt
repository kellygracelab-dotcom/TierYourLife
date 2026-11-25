package com.artiuillab.tieryourlife.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** A cold start waited 1.7 s for App Check and then 1.7 s more for the ID token; the two do not depend on each other. */
class CallerTokensTest {

    @Test
    fun askingForAppCheck_startsTheIdTokenAtTheSameMoment() {
        val idStarted = CountDownLatch(1)
        val appCheck = CompletableFuture<String?>()
        val tokens = CallerTokens(appCheck = { appCheck }, idToken = {
            idStarted.countDown()
            CompletableFuture.completedFuture("id")
        })

        val waiting = thread { tokens.appCheckToken() }

        assertTrue("the ID token waited for App Check", idStarted.await(5, TimeUnit.SECONDS))
        appCheck.complete("check")
        waiting.join(5_000)
    }

    @Test
    fun theIdTokenStartedAlongside_isTheOneTheNextAskWaitsFor() {
        var idFetches = 0
        val id = CompletableFuture<String?>()
        val tokens = CallerTokens(appCheck = { CompletableFuture.completedFuture("check") }, idToken = {
            idFetches++
            id
        })

        assertEquals("check", tokens.appCheckToken())
        val waiting = CompletableFuture.supplyAsync { tokens.idToken() }
        id.complete("id")

        assertEquals("id", waiting.get(5, TimeUnit.SECONDS))
        assertEquals(1, idFetches)
    }

    // A burst of calls at launch makes one exchange, and one guest, not one each.
    @Test
    fun callsWhileATokenIsOnItsWay_shareThatFetch() {
        var fetches = 0
        val pending = CompletableFuture<String?>()
        val tokens = CallerTokens(appCheck = {
            fetches++
            pending
        }, idToken = { CompletableFuture.completedFuture("id") })

        val calls = List(4) { CompletableFuture.supplyAsync { tokens.appCheckToken() } }
        Thread.sleep(100)
        pending.complete("check")

        assertEquals(List(4) { "check" }, calls.map { it.get(5, TimeUnit.SECONDS) })
        assertEquals(1, fetches)
    }

    // Once it is back, the next call asks again; Firebase answers from its own store while the token is valid.
    @Test
    fun aFinishedFetch_isNotReusedForever() {
        var fetches = 0
        val tokens = CallerTokens(appCheck = {
            fetches++
            CompletableFuture.completedFuture("check-$fetches")
        }, idToken = { CompletableFuture.completedFuture("id") })

        tokens.appCheckToken()

        assertEquals("check-2", tokens.appCheckToken())
    }

    @Test
    fun aFailedToken_isNull_andTheCallGoesOnWithout() {
        val tokens = CallerTokens(
            appCheck = { CompletableFuture<String?>().apply { completeExceptionally(IllegalStateException("attestation failed")) } },
            idToken = { error("no Firebase here") },
        )

        assertNull(tokens.appCheckToken())
        assertNull(tokens.idToken())
    }
}
