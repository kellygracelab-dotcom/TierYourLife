package com.artiuillab.tieryourlife

import android.content.Context
import com.artiuillab.tieryourlife.feature.account.domain.signin.GoogleWebClientId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Only this module runs the google-services plugin, so this is the one place
// the generated resource exists.
@Module
@InstallIn(SingletonComponent::class)
object WebClientIdModule {

    @Provides
    @Singleton
    @GoogleWebClientId
    fun provideGoogleWebClientId(@ApplicationContext context: Context): String =
        context.getString(R.string.default_web_client_id)
}
