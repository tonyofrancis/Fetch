package com.tonyodev.fetchapp

import androidx.multidex.BuildConfig
import timber.log.Timber
object TimberUtils {

    @JvmStatic
    fun configTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}