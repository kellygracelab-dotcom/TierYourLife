package com.artiuillab.tieryourlife

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.artiuillab.tieryourlife.core.logging.CrashKeys
import com.artiuillab.tieryourlife.core.logging.Logging
import com.artiuillab.tieryourlife.core.usage.UsageReporter
import com.artiuillab.tieryourlife.feature.account.data.repository.ProfileOnReturn
import com.artiuillab.tieryourlife.feature.account.data.settings.ColourSync
import com.artiuillab.tieryourlife.feature.community.data.warmup.CommunityWarmUp
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

    @Inject
    lateinit var communityWarmUp: CommunityWarmUp

    @Inject
    lateinit var colourSync: ColourSync

    @Inject
    lateinit var profileOnReturn: ProfileOnReturn

    @Inject
    lateinit var usage: UsageReporter

    override fun onCreate() {
        // Before super, which is where Hilt builds the singletons.
        MainThreadChecks.install()
        super.onCreate()
        AppCheckInstaller.install(this)
        Logging.install(debug = BuildConfig.DEBUG, report = BuildConfig.BUILD_TYPE == "release")
        watchIdentity()
        // After Crashlytics: a crash is counted here first, then reported there.
        usage.start()
        // Started here rather than from a screen: the case it exists for is
        // somebody deep inside a board when the signal comes back, with no
        // screen watching that would ever ask.
        syncOnReconnect.start()
        communityWarmUp.start()
        colourSync.start()
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
