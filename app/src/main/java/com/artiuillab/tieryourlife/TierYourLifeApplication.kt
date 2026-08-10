package com.artiuillab.tieryourlife

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Exposes the Hilt-configured ImageLoader as Coil's app-wide singleton.
@HiltAndroidApp
class TierYourLifeApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
