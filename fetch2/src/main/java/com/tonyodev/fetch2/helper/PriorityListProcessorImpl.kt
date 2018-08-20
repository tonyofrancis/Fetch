package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2core.HandlerWrapper
import com.tonyodev.fetch2.provider.DownloadProvider
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.fetch.ListenerCoordinator
import com.tonyodev.fetch2core.Logger
import com.tonyodev.fetch2core.isFetchFileServerUrl

class PriorityListProcessorImpl constructor(private val handlerWrapper: HandlerWrapper,
                                            private val downloadProvider: DownloadProvider,
                                            private val downloadManager: DownloadManager,
                                            private val networkInfoProvider: NetworkInfoProvider,
                                            private val logger: Logger,
                                            private val listenerCoordinator: ListenerCoordinator,
                                            @Volatile
                                            override var downloadConcurrentLimit: Int)
    : PriorityListProcessor<Download> {

    private val lock = Any()
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
        if (canContinueToProcess()) {
            if (downloadManager.canAccommodateNewDownload() && canContinueToProcess()) {
                val priorityList = getPriorityList()
                for (index in 0..priorityList.lastIndex) {
                    if (downloadManager.canAccommodateNewDownload() && canContinueToProcess()) {
                        val download = priorityList[index]
                        val isFetchServerRequest = isFetchFileServerUrl(download.url)
                        if ((isFetchServerRequest || networkInfoProvider.isNetworkAvailable) && canContinueToProcess()) {
                            val networkType = when {
                                globalNetworkType != NetworkType.GLOBAL_OFF -> globalNetworkType
                                download.networkType == NetworkType.GLOBAL_OFF -> NetworkType.ALL
                                else -> download.networkType
                            }
                            val properNetworkConditions = networkInfoProvider.isOnAllowedNetwork(networkType)
                            if (!properNetworkConditions) {
                                listenerCoordinator.mainListener.onWaitingNetwork(download)
                            }
                            if ((isFetchServerRequest || properNetworkConditions) && !downloadManager.contains(download.id)
                                    && canContinueToProcess()) {
                                downloadManager.start(download)
                            }
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }
            }
            registerPriorityIterator()
        }
    }

    override fun start() {
        synchronized(lock) {
            stopped = false
            paused = false
            registerPriorityIterator()
            logger.d("PriorityIterator started")
        }
    }

    override fun stop() {
        synchronized(lock) {
            unregisterPriorityIterator()
            paused = false
            stopped = true
            downloadManager.cancelAll()
            logger.d("PriorityIterator stop")
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
        if (downloadConcurrentLimit > 0) {
            handlerWrapper.postDelayed(priorityIteratorRunnable, PRIORITY_QUEUE_INTERVAL_IN_MILLISECONDS)
        }
    }

    private fun unregisterPriorityIterator() {
        if (downloadConcurrentLimit > 0) {
            handlerWrapper.removeCallbacks(priorityIteratorRunnable)
        }
    }

    private fun canContinueToProcess(): Boolean {
        return !stopped && !paused
    }

}