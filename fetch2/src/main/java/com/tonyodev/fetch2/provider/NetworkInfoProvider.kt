package com.tonyodev.fetch2.provider

import android.content.Context
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.util.isNetworkAvailable
import com.tonyodev.fetch2.util.isOnWiFi


class NetworkInfoProvider constructor(private val context: Context) {

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