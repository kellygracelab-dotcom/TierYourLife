package com.artiuillab.tieryourlife

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Coil resolves its singleton loader through this factory, which is what lets every AsyncImage
// in the app use the network client configured in ImageLoaderModule rather than Coil's own.
// Without it, Wikimedia answers 403 to Coil's default User-Agent and no Wikidata image ever
// appears.
@HiltAndroidApp
class TierYourLifeApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
