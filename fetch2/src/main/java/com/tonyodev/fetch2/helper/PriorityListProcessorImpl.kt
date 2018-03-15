package com.tonyodev.fetch2.helper


import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2.provider.DownloadProvider
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS

class PriorityListProcessorImpl constructor(private val handler: Handler,
                                            private val downloadProvider: DownloadProvider,
                                            private val downloadManager: DownloadManager,
                                            private val networkInfoProvider: NetworkInfoProvider,
                                            private val logger: Logger)
    : PriorityListProcessor<Download> {

    private val lock = Object()
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

    private val priorityIteratorRunnable = Runnable {
        if (canContinueToProcess()) {
            if (networkInfoProvider.isNetworkAvailable) {
                val priorityList = getPriorityList()
                for (index in 0..priorityList.lastIndex) {
                    if (downloadManager.canAccommodateNewDownload() && canContinueToProcess()) {
                        val download = priorityList[index]
                        val networkType = when {
                            globalNetworkType != NetworkType.GLOBAL_OFF -> globalNetworkType
                            download.networkType == NetworkType.GLOBAL_OFF -> NetworkType.ALL
                            else -> download.networkType
                        }
                        if (networkInfoProvider.isOnAllowedNetwork(networkType) && canContinueToProcess()) {
                            downloadManager.start(download)
                        }
                    } else {
                        break
                    }
                }
            }
            if (canContinueToProcess()) {
                registerPriorityIterator()
            }
        }
    }

    private fun canContinueToProcess(): Boolean {
        return !stopped && !paused
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

    override fun getPriorityList(): List<Download> {
        synchronized(lock) {
            return try {
                downloadProvider.getPendingDownloadsSorted()
            } catch (e: Exception) {
                logger.d("PriorityIterator failed access database", e)
                listOf()
            }
        }
    }

    private fun registerPriorityIterator() {
        handler.postDelayed(priorityIteratorRunnable, PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS)
    }

    private fun unregisterPriorityIterator() {
        handler.removeCallbacks(priorityIteratorRunnable)
    }

}