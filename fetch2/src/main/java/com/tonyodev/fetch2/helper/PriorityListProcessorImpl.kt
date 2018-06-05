package com.tonyodev.fetch2.helper


import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2.fetch.HandlerWrapper
import com.tonyodev.fetch2.provider.DownloadProvider
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS
import com.tonyodev.fetch2.util.isFetchFileServerUrl

class PriorityListProcessorImpl constructor(private val handlerWrapper: HandlerWrapper,
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
            if (downloadManager.canAccommodateNewDownload()) {
                val priorityList = getPriorityList()
                for (index in 0..priorityList.lastIndex) {
                    val download = priorityList[index]
                    val isFetchServerRequest = try {
                        isFetchFileServerUrl(download.url)
                    } catch (e: Exception) {
                        false
                    }
                    if (canContinueToProcess() && (isFetchServerRequest || networkInfoProvider.isNetworkAvailable)) {
                        val networkType = when {
                            globalNetworkType != NetworkType.GLOBAL_OFF -> globalNetworkType
                            download.networkType == NetworkType.GLOBAL_OFF -> NetworkType.ALL
                            else -> download.networkType
                        }
                        if ((isFetchServerRequest || networkInfoProvider.isOnAllowedNetwork(networkType)) && canContinueToProcess()
                                && !downloadManager.contains(download.id)) {
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
        handlerWrapper.postDelayed(priorityIteratorRunnable, PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS)
    }

    private fun unregisterPriorityIterator() {
        handlerWrapper.removeCallbacks(priorityIteratorRunnable)
    }

}