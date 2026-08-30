package com.artiuillab.tieryourlife.feature.tier.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** What this phone is connected by, and when that changes. */
interface Connection {

    /**
     * Emits once every time a network becomes usable.
     *
     * This is what makes "it will go up when you are back online" true without
     * a button. Sync otherwise only wakes on the way back to the list, so
     * somebody who lost signal inside a board and got it back would sit there
     * with nothing happening and no way to ask.
     */
    val available: Flow<Unit>

    /**
     * True on anything that is not somebody's data allowance -- Wi-Fi,
     * ethernet, a laptop sharing its connection. Asked the other way round on
     * purpose: an unknown network is treated as metered, because guessing
     * wrong in the other direction spends somebody's money.
     */
    fun unmetered(): Boolean
}

@Singleton
class SystemConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) : Connection {

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

    override fun unmetered(): Boolean {
        val manager = context.getSystemService<ConnectivityManager>() ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
