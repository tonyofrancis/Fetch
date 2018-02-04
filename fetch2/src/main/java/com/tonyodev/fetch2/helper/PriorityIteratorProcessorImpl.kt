package com.tonyodev.fetch2.helper


import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2.provider.DownloadProvider
import com.tonyodev.fetch2.provider.NetworkProvider
import com.tonyodev.fetch2.util.PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS

open class PriorityIteratorProcessorImpl constructor(val handler: Handler,
                                                     val downloadProvider: DownloadProvider,
                                                     val downloadManager: DownloadManager,
                                                     val networkProvider: NetworkProvider,
                                                     val logger: Logger)
    : PriorityIteratorProcessor<Download> {

    val lock = Object()
    @Volatile
    override var globalNetworkType = NetworkType.GLOBAL_OFF
    @Volatile
    private var paused = false
    override val isPaused: Boolean
        get() = paused
    @Volatile
    private var stopped = true
    override val isStopped: Boolean
        get() = stopped

    val priorityIteratorRunnableInternal = Runnable {
        val iterator = getPriorityIterator()
        if (iterator.hasNext()) {
            var hasStartedADownload = false
            while (iterator.hasNext() && downloadManager.canAccommodateNewDownload()) {
                val download = iterator.next()
                val networkType = when {
                    globalNetworkType != NetworkType.GLOBAL_OFF -> globalNetworkType
                    download.networkType == NetworkType.GLOBAL_OFF -> NetworkType.ALL
                    else -> download.networkType
                }
                if (networkProvider.isOnAllowedNetwork(networkType)) {
                    downloadManager.start(download)
                    hasStartedADownload = true
                }
            }
            if (hasStartedADownload) {
                registerPriorityIteratorInternal()
            } else {
                stop()
            }
        } else {
            stop()
        }
    }

    override fun start() {
        synchronized(lock) {
            stopped = false
            paused = false
            registerPriorityIteratorInternal()
        }
    }

    override fun stop() {
        synchronized(lock) {
            unregisterPriorityIteratorInternal()
            paused = false
            stopped = true
        }
    }

    override fun pause() {
        synchronized(lock) {
            unregisterPriorityIteratorInternal()
            paused = true
            stopped = false
            downloadManager.cancelAll()
            logger.d("PriorityIterator paused")
        }
    }

    override fun resume() {
        synchronized(lock) {
            paused = false
            stopped = false
            registerPriorityIteratorInternal()
            logger.d("PriorityIterator resumed")
        }
    }

    override fun getPriorityIterator(): Iterator<Download> {
        synchronized(lock) {
            val queuedStatusList = downloadProvider.getByStatus(Status.QUEUED)
            val mapper = queuedStatusList.groupBy {
                it.priority
            }
            val queue = mutableListOf<Download>()
            val highPriorityList = mapper[Priority.HIGH]
            if (highPriorityList != null) {
                queue.addAll(highPriorityList.sortedBy { it.created })
            }
            val normalPriorityList = mapper[Priority.NORMAL]
            if (normalPriorityList != null) {
                queue.addAll(normalPriorityList.sortedBy { it.created })
            }
            val lowPriorityList = mapper[Priority.LOW]
            if (lowPriorityList != null) {
                queue.addAll(lowPriorityList.sortedBy { it.created })
            }
            return queue.iterator()
        }
    }

    open fun registerPriorityIteratorInternal() {
        handler.postDelayed(priorityIteratorRunnableInternal, PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS)
    }

    open fun unregisterPriorityIteratorInternal() {
        handler.removeCallbacks(priorityIteratorRunnableInternal)
    }

}