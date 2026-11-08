package com.artiuillab.tieryourlife.feature.account.presentation.signin

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class GoogleCredentialRequestTest {

    @Test
    fun anExplicitGoogleButton_usesOnlyTheButtonFlow_andKeepsTheServerClientId() {
        val clientId = "test-server.apps.googleusercontent.com"
        val request = googleButtonSignInRequest(clientId)

        assertEquals(1, request.credentialOptions.size)
        val option = request.credentialOptions.single()
        assertTrue(
            "An explicit button must not request the Google bottom sheet",
            option is GetSignInWithGoogleOption,
        )
        assertEquals(clientId, (option as GetSignInWithGoogleOption).serverClientId)
    }
}
