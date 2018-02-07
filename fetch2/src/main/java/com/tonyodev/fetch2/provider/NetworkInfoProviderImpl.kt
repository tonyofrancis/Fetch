package com.tonyodev.fetch2.provider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.util.isNetworkAvailable
import com.tonyodev.fetch2.util.isOnWiFi

class NetworkInfoProviderImpl constructor(private val context: Context,
                                          private val logger: Logger) : NetworkInfoProvider {

    @Volatile
    private var closed = false
    override val isClosed: Boolean
        get() = closed

    private val lock = Object()
    private val networkConnectivityCallbackSet = mutableSetOf<NetworkInfoProvider.ConnectivityCallback>()
    private val networkBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val isNetworkAvailable = context?.isNetworkAvailable()
                if (isNetworkAvailable != null) {
                    if (isNetworkAvailable) {
                        networkConnectivityCallbackSet.iterator().forEach { it.onConnected() }
                    } else {
                        networkConnectivityCallbackSet.iterator().forEach { it.onDisconnected() }
                    }
                }
            } catch (e: Exception) {
                logger.e("Error in network broadcast receiver", e)
            }
        }
    }

    init {
        context.registerReceiver(networkBroadcastReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        context.unregisterReceiver(networkBroadcastReceiver)
    }

    override fun isOnAllowedNetwork(networkType: NetworkType): Boolean {
        throwExceptionIfClosed()
        if (networkType == NetworkType.WIFI_ONLY && context.isOnWiFi()) {
            return true
        }
        if (networkType == NetworkType.ALL && context.isNetworkAvailable()) {
            return true
        }
        return false
    }

    override val isConnected: Boolean
        get() {
            throwExceptionIfClosed()
            return context.isNetworkAvailable()
        }

    override fun registerConnectivityCallback(callback: NetworkInfoProvider.ConnectivityCallback) {
        synchronized(lock) {
            throwExceptionIfClosed()
            networkConnectivityCallbackSet.add(callback)
        }
    }

    override fun unregisterConnectivityCallback(callback: NetworkInfoProvider.ConnectivityCallback) {
        synchronized(lock) {
            throwExceptionIfClosed()
            val iterator = networkConnectivityCallbackSet.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() == callback) {
                    iterator.remove()
                    break
                }
            }
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchException("This NetworkInfoProvider instance has been closed.",
                    FetchException.Code.CLOSED)
        }
    }
}