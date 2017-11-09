package com.tonyodev.fetch2

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

import java.util.concurrent.TimeUnit

import okhttp3.OkHttpClient

internal object NetworkUtils {

    fun okHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(false)
                .readTimeout(20000, TimeUnit.SECONDS)
                .connectTimeout(15000, TimeUnit.SECONDS)
                .cache(null)
                .build()
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}
