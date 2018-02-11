package com.tonyodev.fetch2.helper


import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2.provider.DownloadProvider
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS

class PriorityIteratorProcessorImpl constructor(private val handler: Handler,
                                                private val downloadProvider: DownloadProvider,
                                                private val downloadManager: DownloadManager,
                                                private val networkInfoProvider: NetworkInfoProvider,
                                                private val logger: Logger)
    : PriorityIteratorProcessor<Download> {

    private val lock = Object()

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

    private val priorityIteratorRunnable = Runnable {
        if (networkInfoProvider.isConnected) {
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
                    if (networkInfoProvider.isOnAllowedNetwork(networkType)) {
                        downloadManager.start(download)
                        hasStartedADownload = true
                    }
                }
                if (hasStartedADownload) {
                    registerPriorityIterator()
                } else {
                    stop()
                }
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
            registerPriorityIterator()
        }
    }

    override fun stop() {
        synchronized(lock) {
            unregisterPriorityIterator()
            paused = false
            stopped = true
        }
    }

    override fun pause() {
        synchronized(lock) {
            unregisterPriorityIterator()
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
            registerPriorityIterator()
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

    private fun registerPriorityIterator() {
        handler.postDelayed(priorityIteratorRunnable, PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS)
    }

    private fun unregisterPriorityIterator() {
        handler.removeCallbacks(priorityIteratorRunnable)
    }

}