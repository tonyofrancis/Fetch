package com.tonyodev.fetch2.provider

import com.tonyodev.fetch2.NetworkType
import java.io.Closeable

interface NetworkInfoProvider : Closeable {

    val isNetworkConnected: Boolean

    fun isOnAllowedNetwork(networkType: NetworkType): Boolean

    fun registerCallbackForNetworkConnection(callback: NetworkConnectivityCallback)

    fun unregisterCallbackForNetworkConnection(callback: NetworkConnectivityCallback)

    interface NetworkConnectivityCallback {
        fun onConnected()
        fun onDisconnected()
    }
}