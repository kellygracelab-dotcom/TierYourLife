package com.artiuillab.tieryourlife

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.artiuillab.tieryourlife.core.logging.CrashKeys
import com.artiuillab.tieryourlife.core.logging.Logging
import com.artiuillab.tieryourlife.feature.tier.data.sync.SyncOnReconnect
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Exposes the Hilt-configured ImageLoader as Coil's app-wide singleton.
@HiltAndroidApp
class TierYourLifeApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var syncOnReconnect: SyncOnReconnect

    override fun onCreate() {
        super.onCreate()
        AppCheckInstaller.install(this)
        Logging.install(debug = BuildConfig.DEBUG)
        watchIdentity()
        // Started here rather than from a screen: the case it exists for is
        // somebody deep inside a board when the signal comes back, with no
        // screen watching that would ever ask.
        syncOnReconnect.start()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    private fun watchIdentity() {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            Logging.setUser(user?.uid)
            Logging.setKey(CrashKeys.SIGNED_IN, (user != null && !user.isAnonymous).toString())
        }
    }
}
