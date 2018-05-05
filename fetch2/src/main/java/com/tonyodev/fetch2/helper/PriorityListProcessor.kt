package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.NetworkType


interface PriorityListProcessor<out T> {

    var globalNetworkType: NetworkType
    val isPaused: Boolean
    val isStopped: Boolean

    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun getPriorityList(): List<T>

}