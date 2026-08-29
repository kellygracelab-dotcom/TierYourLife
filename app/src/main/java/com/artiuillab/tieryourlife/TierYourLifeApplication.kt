package com.artiuillab.tieryourlife

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.artiuillab.tieryourlife.core.logging.CrashKeys
import com.artiuillab.tieryourlife.core.logging.Logging
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Exposes the Hilt-configured ImageLoader as Coil's app-wide singleton.
@HiltAndroidApp
class TierYourLifeApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        AppCheckInstaller.install(this)
        Logging.install(debug = BuildConfig.DEBUG)
        watchIdentity()
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
