@file:JvmName("OkHttpUtils")

package com.tonyodev.fetch2okhttp

import com.tonyodev.fetch2core.getDefaultCookieManager
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar

fun getDefaultCookieJar(): CookieJar {
    val cookieManager = getDefaultCookieManager()
    return JavaNetCookieJar(cookieManager)
}