package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.Publishing
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PublishAfterSignInTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var signedIn by mutableStateOf(false)
    private var signIns = 0
    private val sent = mutableListOf<Publishing>()
    private lateinit var tap: (Publishing) -> Unit

    private fun show() {
        composeRule.setContent {
            tap = publishAfterSignIn(signedIn = signedIn, onSignIn = { signIns++ }) { sent += it }
        }
    }

    @Test
    fun signedOut_aTapAsksToSignIn_andThePublishGoesOnOnceSignedIn() {
        show()

        tap(Publishing.ShareRanking)
        composeRule.waitForIdle()
        assertEquals(1, signIns)
        assertEquals(emptyList<Publishing>(), sent)

        signedIn = true
        composeRule.waitForIdle()

        assertEquals(listOf(Publishing.ShareRanking), sent)
        signedIn = false
        composeRule.waitForIdle()
        signedIn = true
        composeRule.waitForIdle()
        assertEquals("once, not again on the next sign-in", listOf(Publishing.ShareRanking), sent)
    }

    @Test
    fun signedIn_aTapPublishesAtOnce() {
        signedIn = true
        show()

        tap(Publishing.Publish)
        composeRule.waitForIdle()

        assertEquals(0, signIns)
        assertEquals(listOf(Publishing.Publish), sent)
    }
}
