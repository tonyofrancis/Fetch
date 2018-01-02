package com.tonyodev.fetch2.helper


import android.os.Handler
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2.provider.DownloadProvider
import com.tonyodev.fetch2.provider.NetworkProvider
import com.tonyodev.fetch2.util.PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS
import java.util.PriorityQueue
import java.util.Queue

open class PriorityQueueProcessorImpl constructor(val handler: Handler,
                                                  val downloadProvider: DownloadProvider,
                                                  val downloadManager: DownloadManager,
                                                  val networkProvider: NetworkProvider,
                                                  val logger: Logger)
    : PriorityQueueProcessor<Download> {

    val lock = Object()
    @Volatile
    override var globalNetworkType = NetworkType.GLOBAL_OFF
    @Volatile
    private var paused = false
    override val isPaused: Boolean
        get() = paused
    @Volatile
    private var stopped = false
    override val isStopped: Boolean
        get() = stopped
    val priorityComparatorInternal = DownloadPriorityComparator()
    val priorityQueueInternal = PriorityQueue<Download>(16, priorityComparatorInternal)

    val priorityQueueRunnableInternal = Runnable {
        val queue = getPriorityQueue()
        while (queue.isNotEmpty() && downloadManager.canAccommodateNewDownload()) {
            val download = queue.poll()
            if (download != null) {
                val networkType = when {
                    globalNetworkType != NetworkType.GLOBAL_OFF -> globalNetworkType
                    download.networkType == NetworkType.GLOBAL_OFF -> NetworkType.ALL
                    else -> download.networkType
                }
                if (networkProvider.isOnAllowedNetwork(networkType)) {
                    downloadManager.start(download)
                }
            }
        }
        priorityQueueInternal.clear()
        registerPriorityQueueInternal()
    }

    override fun start() {
        synchronized(lock) {
            stopped = false
            paused = false
            registerPriorityQueueInternal()
        }
    }

    override fun stop() {
        synchronized(lock) {
            unregisterPriorityQueueInternal()
            priorityQueueInternal.clear()
            paused = false
            stopped = true
        }
    }

    override fun pause() {
        synchronized(lock) {
            unregisterPriorityQueueInternal()
            paused = true
            stopped = false
            downloadManager.cancelAll()
            logger.d("PriorityQueue paused")
        }
    }

    override fun resume() {
        synchronized(lock) {
            paused = false
            stopped = false
            registerPriorityQueueInternal()
            logger.d("PriorityQueue resumed")
        }
    }

    override fun getPriorityQueue(): Queue<Download> {
        synchronized(lock) {
            val queuedStatusList = downloadProvider.getByStatus(Status.QUEUED)
            priorityQueueInternal.clear()
            priorityQueueInternal.addAll(queuedStatusList)
            return priorityQueueInternal
        }
    }

    open fun registerPriorityQueueInternal() {
        handler.postDelayed(priorityQueueRunnableInternal, PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS)
    }

    open fun unregisterPriorityQueueInternal() {
        handler.removeCallbacks(priorityQueueRunnableInternal)
    }

}