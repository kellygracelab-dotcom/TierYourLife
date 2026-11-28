package com.artiuillab.tieryourlife.feature.tier.data.di

import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PublicMediaInterceptor
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureClientTest {

    @Test
    fun pictures_waitLongEnoughForTheWebToMakeACopy_andComeManyAtOnce() {
        val base = OkHttpClient()

        val client = pictureClient(base)

        assertEquals(30_000, client.readTimeoutMillis)
        assertEquals(base.connectTimeoutMillis, client.connectTimeoutMillis)
        assertEquals(PICTURES_AT_ONCE_PER_HOST, client.dispatcher.maxRequestsPerHost)
        assertTrue(client.interceptors.any { it is PublicMediaInterceptor })
    }
}
