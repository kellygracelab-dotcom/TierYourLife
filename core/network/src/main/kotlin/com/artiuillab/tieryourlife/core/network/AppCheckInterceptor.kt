package com.artiuillab.tieryourlife.core.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

private const val APP_CHECK_HEADER = "X-Firebase-AppCheck"

class AppCheckInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Without this header the proxy turns every call away, so a failure here
        // shows up as "nothing works" rather than as itself.
        val token = runCatching {
            Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(false)).token
        }.onFailure { Timber.w(it, "App Check token unavailable") }.getOrNull()

        val request = if (token.isNullOrEmpty()) {
            chain.request()
        } else {
            chain.request().newBuilder().header(APP_CHECK_HEADER, token).build()
        }
        return chain.proceed(request)
    }
}
