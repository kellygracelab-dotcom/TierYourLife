package com.artiuillab.tieryourlife.feature.aistudio.data.remote.api

import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import okhttp3.Interceptor
import okhttp3.Response

private const val APP_CHECK_HEADER = "X-Firebase-AppCheck"

class AppCheckInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runCatching {
            Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(false)).token
        }.getOrNull()

        val request = if (token.isNullOrEmpty()) {
            chain.request()
        } else {
            chain.request().newBuilder().header(APP_CHECK_HEADER, token).build()
        }
        return chain.proceed(request)
    }
}
