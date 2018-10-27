package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.NetworkType
import java.io.Closeable


interface PriorityListProcessor<out T>: Closeable {

    var downloadConcurrentLimit: Int
    var globalNetworkType: NetworkType
    val isPaused: Boolean
    val isStopped: Boolean

    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun getPriorityList(): List<T>
    fun resetBackOffTime()
    fun sendBackOffResetSignal()

}