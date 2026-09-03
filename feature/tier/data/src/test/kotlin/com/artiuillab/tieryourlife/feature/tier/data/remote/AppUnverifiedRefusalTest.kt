package com.artiuillab.tieryourlife.feature.tier.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AppUnverifiedRefusalTest {

    @Test
    fun theRefusalAboutTheInstallation_isRecognised() {
        assertTrue(refusal(401, """{"error":"Missing App Check token","code":"APP_UNVERIFIED"}""").isAppUnverified())
    }

    // The other 401. Signing in again fixes this one, so it must not be read
    // as the one nothing fixes.
    @Test
    fun theRefusalAboutTheSignIn_isNot() {
        assertFalse(refusal(401, """{"error":"Invalid ID token","code":"UNAUTHENTICATED"}""").isAppUnverified())
    }

    @Test
    fun aBodyThatWillNotParse_isNot() {
        assertFalse(refusal(401, "<html>gateway</html>").isAppUnverified())
    }

    @Test
    fun theSameCodeUnderAnotherStatus_isNot() {
        assertFalse(refusal(403, """{"code":"APP_UNVERIFIED"}""").isAppUnverified())
    }

    @Test
    fun somethingThatNeverReachedTheServer_isNot() {
        assertFalse(IOException("offline").isAppUnverified())
    }

    private fun refusal(code: Int, body: String) =
        HttpException(Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType())))
}
