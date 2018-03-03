package com.tonyodev.fetch2.provider

import com.tonyodev.fetch2.NetworkType
import java.io.Closeable

interface NetworkInfoProvider : Closeable {

    val isClosed: Boolean

    val isConnected: Boolean

    fun isOnAllowedNetwork(networkType: NetworkType): Boolean

    fun registerConnectivityCallback(callback: ConnectivityCallback)

    fun unregisterConnectivityCallback(callback: ConnectivityCallback)

    interface ConnectivityCallback {
        fun onConnected()
        fun onDisconnected()
    }

}