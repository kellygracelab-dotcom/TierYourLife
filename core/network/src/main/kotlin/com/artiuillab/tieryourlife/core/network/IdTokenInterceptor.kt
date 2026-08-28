package com.artiuillab.tieryourlife.core.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

private const val AUTHORIZATION_HEADER = "Authorization"

/**
 * Names the install to the backend, which is what its generation quota is
 * counted against. App Check already proves the request came from this app;
 * it cannot say which copy of it.
 *
 * Signing in happens here rather than at startup so the first network call
 * cannot race the bootstrap. It is anonymous: no screen, no account, nothing
 * asked of the user. Blocking is fine — interceptors never run on the main thread.
 */
class IdTokenInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runCatching {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: Tasks.await(auth.signInAnonymously()).user
            user?.let { Tasks.await(it.getIdToken(false)).token }
        }.getOrNull()

        val request = if (token.isNullOrEmpty()) {
            chain.request()
        } else {
            chain.request().newBuilder().header(AUTHORIZATION_HEADER, "Bearer $token").build()
        }
        return chain.proceed(request)
    }
}
