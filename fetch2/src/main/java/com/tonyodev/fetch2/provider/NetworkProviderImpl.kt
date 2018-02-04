package com.tonyodev.fetch2.provider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.util.isNetworkAvailable
import com.tonyodev.fetch2.util.isOnWiFi

open class NetworkProviderImpl constructor(private val context: Context) : NetworkProvider {

    private val lock = Object()
    private val networkConnectivityCallbackSet = mutableSetOf<NetworkProvider.NetworkConnectivityCallback>()
    private val networkBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (this@NetworkProviderImpl.context.isNetworkAvailable()) {
                networkConnectivityCallbackSet.iterator().forEach { it.onConnected() }
            } else {
                networkConnectivityCallbackSet.iterator().forEach { it.onDisconnected() }
            }
        }
    }

    init {
        context.registerReceiver(networkBroadcastReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun close() {
        context.unregisterReceiver(networkBroadcastReceiver)
    }

    override fun isOnAllowedNetwork(networkType: NetworkType): Boolean {
        if (networkType == NetworkType.WIFI_ONLY && context.isOnWiFi()) {
            return true
        }
        if (networkType == NetworkType.ALL && context.isNetworkAvailable()) {
            return true
        }
        return false
    }

    override val isNetworkConnected: Boolean = context.isNetworkAvailable()

    override fun registerCallbackForNetworkConnection(callback: NetworkProvider.NetworkConnectivityCallback) {
        synchronized(lock) {
            networkConnectivityCallbackSet.add(callback)
        }
    }

    override fun unregisterCallbackForNetworkConnection(callback: NetworkProvider.NetworkConnectivityCallback) {
        synchronized(lock) {
            val iterator = networkConnectivityCallbackSet.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() == callback) {
                    iterator.remove()
                    break
                }
            }
        }
    }
}