package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.NetworkType
import java.util.*

interface PriorityQueueProcessor<T> {

    var globalNetworkType: NetworkType
    val isPaused: Boolean
    val isStopped: Boolean

    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun getPriorityQueue(): Queue<T>

}