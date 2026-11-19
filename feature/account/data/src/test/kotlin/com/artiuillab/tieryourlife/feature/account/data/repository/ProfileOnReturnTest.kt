package com.artiuillab.tieryourlife.feature.account.data.repository

import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileOnReturnTest {

    // Danylo, 19.11: a face worn on the site did not show in the app, which kept the face it signed in with.
    @Test
    fun aFaceChangedOnAnotherDevice_showsWhenTheAppIsBack_readAtMostEveryHalfMinute() {
        var clock = 1_000_000L
        val accounts = FakeAccountRepository(Account.SignedIn(email = "ann@example.com", photoUrl = "https://a/old.png", uid = "ann"))
        val onReturn = ProfileOnReturn(accounts, CoroutineScope(Dispatchers.Unconfined)) { clock }
        accounts.elsewhere = Account.SignedIn(email = "ann@example.com", photoUrl = "https://a/site.png", uid = "ann")

        onReturn.backOnScreen()
        assertEquals("https://a/site.png", (accounts.current() as Account.SignedIn).photoUrl)

        clock += 10_000
        onReturn.backOnScreen()
        assertEquals(1, accounts.profileReads)

        clock += 30_000
        onReturn.backOnScreen()
        assertEquals(2, accounts.profileReads)
    }
}
