package com.artiuillab.tieryourlife

import com.artiuillab.tieryourlife.core.usage.UsageInstall
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UsageInstallModule {

    /** The build number: every build is "1.0", and the number is what tells them apart. */
    @Provides
    fun provideUsageInstall(): UsageInstall = UsageInstall(version = BuildConfig.VERSION_CODE.toString())
}
