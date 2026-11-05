package com.artiuillab.tieryourlife.feature.community.presentation.follow

import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowAfterSignInTest {

    private val author = FollowTarget.Author("ann")
    private var now = 1_000L

    @Test
    fun signedIn_followsAtOnce_andNothingWaits() = runBlocking {
        val accounts = FakeAccountRepository(FakeAccountRepository.signedIn)
        val follows = FollowAfterSignIn(accounts) { now }

        assertTrue(follows.followNowOrWait(author))
        assertNull(withTimeoutOrNull(QUIET_MILLIS) { follows.signedIn { true }.first() })
    }

    @Test
    fun aGuest_waits_andTheSignInHandsTheTargetOverOnce() = runBlocking {
        val accounts = FakeAccountRepository()
        val follows = FollowAfterSignIn(accounts) { now }

        assertFalse(follows.followNowOrWait(author))
        now += FOLLOW_AFTER_SIGN_IN_MILLIS
        accounts.accounts.value = FakeAccountRepository.signedIn

        assertEquals(author, withTimeout(WAIT_MILLIS) { follows.signedIn { true }.first() })
        accounts.accounts.value = Account.SignedIn(email = "again@example.com", photoUrl = null)
        assertNull(withTimeoutOrNull(QUIET_MILLIS) { follows.signedIn { true }.first() })
    }

    @Test
    fun aSignInLongerThanTenMinutes_forgetsIt() = runBlocking {
        val accounts = FakeAccountRepository()
        val follows = FollowAfterSignIn(accounts) { now }

        follows.followNowOrWait(author)
        now += FOLLOW_AFTER_SIGN_IN_MILLIS + 1
        accounts.accounts.value = FakeAccountRepository.signedIn

        assertNull(withTimeoutOrNull(QUIET_MILLIS) { follows.signedIn { true }.first() })
    }

    @Test
    fun aScreenTakesOnlyWhatItAccepts_andTheLatestTapIsTheOneThatWaits() = runBlocking {
        val accounts = FakeAccountRepository()
        val follows = FollowAfterSignIn(accounts) { now }
        val section = FollowTarget.Section("food")

        follows.followNowOrWait(author)
        follows.followNowOrWait(section)
        accounts.accounts.value = FakeAccountRepository.signedIn

        assertNull(withTimeoutOrNull(QUIET_MILLIS) { follows.signedIn { it is FollowTarget.Author }.first() })
        assertEquals(listOf(section), withTimeout(WAIT_MILLIS) { follows.signedIn { it == section }.take(1).toList() })
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L
        const val QUIET_MILLIS = 200L
    }
}
