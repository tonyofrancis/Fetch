package com.tonyodev.fetch2.provider

import android.content.Context
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.util.isNetworkAvailable
import com.tonyodev.fetch2.util.isOnWiFi


open class NetworkProvider constructor(val contextInternal: Context) {

    open fun isOnAllowedNetwork(networkType: NetworkType): Boolean {
        if (networkType == NetworkType.WIFI_ONLY && contextInternal.isOnWiFi()) {
            return true
        }
        if (networkType == NetworkType.ALL && contextInternal.isNetworkAvailable()) {
            return true
        }
        return false
    }

}