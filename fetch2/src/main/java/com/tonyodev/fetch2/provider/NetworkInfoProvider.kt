package com.tonyodev.fetch2.provider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2core.isNetworkAvailable
import com.tonyodev.fetch2core.isOnWiFi


class NetworkInfoProvider constructor(private val context: Context) {

    private val lock = Any()
    private val networkChangeListenerSet = hashSetOf<NetworkChangeListener>()
    private val connectivityManager: ConnectivityManager? = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val networkChangeBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            notifyNetworkChangeListeners()
        }
    }
    private var broadcastRegistered = false
    private var networkCallback: Any? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && connectivityManager != null) {
            val networkRequest = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                    .build()
            val networkCallback: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {

                override fun onLost(network: Network?) {
                    notifyNetworkChangeListeners()
                }

                override fun onAvailable(network: Network?) {
                    notifyNetworkChangeListeners()
                }
            }
            this.networkCallback = networkCallback
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } else {
            try {
                context.registerReceiver(networkChangeBroadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
                broadcastRegistered = true
            } catch (e: Exception) {

            }
        }
    }

    private fun notifyNetworkChangeListeners() {
        synchronized(lock) {
            networkChangeListenerSet.iterator().forEach { listener ->
                listener.onNetworkChanged()
            }
        }
    }

    fun registerNetworkChangeListener(networkChangeListener: NetworkChangeListener) {
        synchronized(lock) {
            networkChangeListenerSet.add(networkChangeListener)
        }
    }

    fun unregisterNetworkChangeListener(networkChangeListener: NetworkChangeListener) {
        synchronized(lock) {
            networkChangeListenerSet.remove(networkChangeListener)
        }
    }

    fun unregisterAllNetworkChangeListeners() {
        synchronized(lock) {
            networkChangeListenerSet.clear()
            if (broadcastRegistered) {
                try {
                    context.unregisterReceiver(networkChangeBroadcastReceiver)
                } catch (e: Exception) {

                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && connectivityManager != null) {
                val networkCallback = this.networkCallback
                if (networkCallback is ConnectivityManager.NetworkCallback) {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
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

    interface NetworkChangeListener {
        fun onNetworkChanged()
    }

}