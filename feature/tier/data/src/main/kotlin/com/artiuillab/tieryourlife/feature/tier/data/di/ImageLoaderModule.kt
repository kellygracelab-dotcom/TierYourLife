package com.artiuillab.tieryourlife.feature.tier.data.di

import android.app.ActivityManager
import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.LargerCollage
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PublicMediaInterceptor
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.SmallCardCopies
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

internal const val PICTURES_AT_ONCE_PER_HOST = 16

/** Coil's own folder until 28.11, kept by address: Backend then put bigger originals at the same addresses. */
internal const val REPLACED_PICTURE_CACHE = "coil3_disk_cache"
internal const val PICTURE_CACHE = "pictures_2"

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
                add(LargerCollage())
                add(OkHttpNetworkFetcherFactory(callFactory = { imageClient }))
            }
            // Half the memory per opaque picture where the phone says it has little: a board is hundreds of them.
            .allowRgb565(lowRam)
            .diskCache { pictureDiskCache(context.cacheDir) }
            .build()
    }
}

/** Opened on Coil's own thread at the first read from disk, so the old folder goes with no screen waiting. */
internal fun pictureDiskCache(cacheDir: File): DiskCache {
    cacheDir.resolve(REPLACED_PICTURE_CACHE).deleteRecursively()
    return DiskCache.Builder().directory(cacheDir.resolve(PICTURE_CACHE)).build()
}
