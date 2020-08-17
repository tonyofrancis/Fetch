@file:JvmName("FetchAndroidExtensions")

package com.tonyodev.fetch2core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build


fun Context.isOnWiFi(): Boolean {
    val manager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = manager.activeNetworkInfo
    return if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
        activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
    } else {
        false
    }
}

fun Context.isOnMeteredConnection(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= 16) {
        cm.isActiveNetworkMetered
    } else {
        val info: NetworkInfo = cm.activeNetworkInfo ?: return true
        when (info.type) {
            ConnectivityManager.TYPE_MOBILE,
            ConnectivityManager.TYPE_MOBILE_DUN,
            ConnectivityManager.TYPE_MOBILE_HIPRI,
            ConnectivityManager.TYPE_MOBILE_MMS,
            ConnectivityManager.TYPE_MOBILE_SUPL,
            ConnectivityManager.TYPE_WIMAX -> true
            ConnectivityManager.TYPE_WIFI,
            ConnectivityManager.TYPE_BLUETOOTH,
            ConnectivityManager.TYPE_ETHERNET -> false
            else -> true
        }
    }
}

fun Context.isNetworkAvailable(): Boolean {
    val manager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = manager.activeNetworkInfo
    var connected = activeNetworkInfo != null && activeNetworkInfo.isConnected
    if (!connected) {
        connected = manager.allNetworkInfo?.any { it.isConnected } ?: false
    }
    return connected
}