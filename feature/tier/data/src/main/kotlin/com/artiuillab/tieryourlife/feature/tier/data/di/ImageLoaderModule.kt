package com.artiuillab.tieryourlife.feature.tier.data.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

// Coil builds its own OkHttpClient by default, and that client sends okhttp's stock
// User-Agent. Wikimedia refuses it: measured against the live host, a Commons thumbnail
// answers 403 to "okhttp/4.12.0" and 200 to a descriptive agent. Every Wikidata image in the
// app therefore failed to load, silently — the search worked, the rows appeared, and the
// pictures were simply never there.
//
// So the image loader is given this module's OkHttpClient, the one already carrying the
// Wikimedia User-Agent interceptor. Sharing it is safe by construction: the TMDB
// Authorization header is gated on TMDB's host and cannot travel to Commons.
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .build()
    }
}
