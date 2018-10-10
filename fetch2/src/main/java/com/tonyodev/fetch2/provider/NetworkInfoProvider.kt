package com.tonyodev.fetch2.provider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2core.isNetworkAvailable
import com.tonyodev.fetch2core.isOnWiFi


class NetworkInfoProvider constructor(private val context: Context) {

    private val lock = Any()
    private val broadcastReceiverSet = mutableSetOf<BroadcastReceiver>()

    fun registerNetworkBroadcastReceiver(broadcastReceiver: BroadcastReceiver) {
        synchronized(lock) {
            val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            try {
                broadcastReceiverSet.add(broadcastReceiver)
                context.registerReceiver(broadcastReceiver, intentFilter)
            } catch (e: Exception) {

            }
        }
    }

    fun unregisterNetworkBroadcastReceiver(broadcastReceiver: BroadcastReceiver) {
        synchronized(lock) {
            try {
                broadcastReceiverSet.remove(broadcastReceiver)
                context.unregisterReceiver(broadcastReceiver)
            } catch (e: Exception) {

            }
        }
    }

    fun unregisterAllNetworkBroadcastReceivers() {
        synchronized(lock) {
            val broadcastReceivers = broadcastReceiverSet.toList()
            broadcastReceivers.forEach { broadcastReceiver ->
                try {
                    broadcastReceiverSet.remove(broadcastReceiver)
                    context.unregisterReceiver(broadcastReceiver)
                } catch (e: Exception) {

                }
            }
        }
    }

    fun isOnAllowedNetwork(networkType: NetworkType): Boolean {
        if (networkType == NetworkType.WIFI_ONLY && context.isOnWiFi()) {
            return true
        }
        if (networkType == NetworkType.ALL && context.isNetworkAvailable()) {
            return true
        }
        return false
    }

    val isNetworkAvailable: Boolean
        get() {
            return context.isNetworkAvailable()
        }

}