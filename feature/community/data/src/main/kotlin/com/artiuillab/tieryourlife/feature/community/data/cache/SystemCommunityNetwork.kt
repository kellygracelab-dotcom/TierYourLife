package com.artiuillab.tieryourlife.feature.community.data.cache

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityNetwork
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemCommunityNetwork @Inject constructor(
    @ApplicationContext private val context: Context,
) : CommunityNetwork {

    override val available: Flow<Unit> = callbackFlow {
        val manager = context.getSystemService<ConnectivityManager>()
        if (manager == null) {
            close()
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(Unit)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
}
