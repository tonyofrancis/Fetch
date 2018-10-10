package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.NetworkType


interface PriorityListProcessor<out T> {

    var downloadConcurrentLimit: Int
    var globalNetworkType: NetworkType
    val isPaused: Boolean
    val isStopped: Boolean
    var delegate: Delegate?

    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun getPriorityList(): List<T>
    fun resetBackOffTime()

    interface Delegate {

        fun onHasActiveDownloads(hasActiveDownloads: Boolean)

    }

}