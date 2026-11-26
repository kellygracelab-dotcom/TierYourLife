package com.artiuillab.tieryourlife.feature.tier.data.di

import android.app.ActivityManager
import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PublicMediaInterceptor
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.SmallCardCopies
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import javax.inject.Singleton

internal const val PICTURES_AT_ONCE_PER_HOST = 16

@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    // Reuses the client that adds Wikimedia's required User-Agent, which upload.wikimedia.org needs too.
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader {
        val lowRam = context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
        val imageClient = okHttpClient.newBuilder()
            // Its own queue, wider than OkHttp's five to a host: a grid of posters from one
            // catalogue came five at a time, where the site's browser asks for all of them at once.
            .dispatcher(Dispatcher().apply { maxRequestsPerHost = PICTURES_AT_ONCE_PER_HOST })
            .addInterceptor(PublicMediaInterceptor())
            .build()
        return ImageLoader.Builder(context)
            .components {
                add(SmallCardCopies())
                add(OkHttpNetworkFetcherFactory(callFactory = { imageClient }))
            }
            // Half the memory per opaque picture where the phone says it has little: a board is hundreds of them.
            .allowRgb565(lowRam)
            .build()
    }
}
