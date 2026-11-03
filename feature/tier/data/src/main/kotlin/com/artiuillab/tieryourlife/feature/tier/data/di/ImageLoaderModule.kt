package com.artiuillab.tieryourlife.feature.tier.data.di

import android.app.ActivityManager
import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    // Reuses the client that adds Wikimedia's required User-Agent.
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader {
        val lowRam = context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            // Half the memory per opaque picture where the phone says it has little: a board is hundreds of them.
            .allowRgb565(lowRam)
            .build()
    }
}
