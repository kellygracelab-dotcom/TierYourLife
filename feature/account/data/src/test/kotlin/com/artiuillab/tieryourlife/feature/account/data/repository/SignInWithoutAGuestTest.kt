package com.artiuillab.tieryourlife.feature.account.data.repository

import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class SignInWithoutAGuestTest {

    @Test
    fun aBrandNewAccount_isThisPhoneBecomingOne() {
        assertEquals(SignInOutcome.Success, outcomeWithoutAGuest(isNewUser = true))
    }

    // Read as a guest turned into the account, it moved every board made
    // without one into somebody's account unasked.
    @Test
    fun anAccountUsedBefore_isAskedAbout_withNoCreditsLost() {
        assertEquals(SignInOutcome.SignedInToExistingAccount(guestCreditsLost = false), outcomeWithoutAGuest(isNewUser = false))
    }

    @Test
    fun notKnowingWhetherItIsNew_asksRatherThanMovesBoards() {
        assertEquals(SignInOutcome.SignedInToExistingAccount(guestCreditsLost = false), outcomeWithoutAGuest(isNewUser = null))
    }
}
