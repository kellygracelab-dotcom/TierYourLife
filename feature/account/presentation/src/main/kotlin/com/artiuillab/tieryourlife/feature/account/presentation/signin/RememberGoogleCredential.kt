package com.artiuillab.tieryourlife.feature.account.presentation.signin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface GoogleCredentialEntryPoint {
    fun googleCredential(): GoogleCredential
}

/** The picker is opened from the screen, which has the activity; the view model only hears the answer. */
@Composable
internal fun rememberGoogleCredential(): GoogleCredential {
    val application = LocalContext.current.applicationContext
    return remember(application) {
        EntryPointAccessors.fromApplication(application, GoogleCredentialEntryPoint::class.java).googleCredential()
    }
}
