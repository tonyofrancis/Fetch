package com.tonyodev.fetch2.downloader

import android.os.Handler
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Downloader
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.exception.FetchImplementationException
import com.tonyodev.fetch2.helper.DownloadInfoUpdater
import com.tonyodev.fetch2.helper.FileDownloaderDelegate
import com.tonyodev.fetch2.provider.ListenerProvider
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import java.util.concurrent.Executors

class DownloadManagerImpl(private val downloader: Downloader,
                          private val concurrentLimit: Int,
                          private val progressReportingIntervalMillis: Long,
                          private val downloadBufferSizeBytes: Int,
                          private val logger: Logger,
                          private val networkInfoProvider: NetworkInfoProvider,
                          private val retryOnNetworkGain: Boolean,
                          private val fetchListenerProvider: ListenerProvider,
                          private val uiHandler: Handler,
                          private val downloadInfoUpdater: DownloadInfoUpdater) : DownloadManager {

    private val lock = Object()
    private val executor = Executors.newFixedThreadPool(concurrentLimit)
    private val currentDownloadsMap = hashMapOf<Int, FileDownloader>()
    @Volatile
    private var downloadCounter = 0
    @Volatile
    private var closed = false
    override val isClosed: Boolean
        get() = closed

    override fun start(download: Download): Boolean {
        synchronized(lock) {
            throwExceptionIfClosed()
            if (currentDownloadsMap.containsKey(download.id)) {
                logger.d("DownloadManager already running download $download")
                return false
            }
            if (downloadCounter >= concurrentLimit) {
                logger.d("DownloadManager cannot init download $download because " +
                        "the download queue is full")
                return false
            }
            val fileDownloader = getNewFileDownloaderForDownload(download)
            fileDownloader.delegate = getFileDownloaderDelegate()
            downloadCounter += 1
            currentDownloadsMap[download.id] = fileDownloader
            return try {
                executor.execute {
                    logger.d("DownloadManager starting download $download")
                    fileDownloader.run()
                    synchronized(lock) {
                        if (currentDownloadsMap.containsKey(download.id)) {
                            currentDownloadsMap.remove(download.id)
                            downloadCounter -= 1
                        }
                    }
                }
                true
            } catch (e: Exception) {
                logger.e("DownloadManager failed to start download $download", e)
                false
            }
        }
    }

    override fun cancel(id: Int): Boolean {
        synchronized(lock) {
            throwExceptionIfClosed()
            return if (currentDownloadsMap.containsKey(id)) {
                val fileDownloader = currentDownloadsMap[id] as FileDownloader
                fileDownloader.interrupted = true
                while (!fileDownloader.terminated) {
                    //Wait until download runnable terminates
                }
                currentDownloadsMap.remove(id)
                downloadCounter -= 1
                logger.d("DownloadManager cancelled download ${fileDownloader.download}")
                true
            } else {
                false
            }
        }
    }

    override fun cancelAll() {
        synchronized(lock) {
            throwExceptionIfClosed()
            cancelAllDownloads()
        }
    }

    private fun cancelAllDownloads() {
        currentDownloadsMap.iterator().forEach {
            it.value.interrupted = true
            while (!it.value.terminated) {
                //Wait until download runnable terminates
            }
            logger.d("DownloadManager cancelled download ${it.value.download}")
        }
        currentDownloadsMap.clear()
        downloadCounter = 0
    }

    override fun close() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
            logger.d("DownloadManager closing download manager")
            cancelAllDownloads()
            executor.shutdown()
            downloader.close()
        }
    }

    override fun contains(id: Int): Boolean {
        synchronized(lock) {
            throwExceptionIfClosed()
            return currentDownloadsMap.containsKey(id)
        }
    }

    override fun canAccommodateNewDownload(): Boolean {
        synchronized(lock) {
            throwExceptionIfClosed()
            return downloadCounter < concurrentLimit
        }
    }

    override fun getActiveDownloadCount(): Int {
        synchronized(lock) {
            throwExceptionIfClosed()
            return downloadCounter
        }
    }

    override fun getDownloads(): List<Download> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return currentDownloadsMap.values.map { it.download }
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchImplementationException("DownloadManager is already shutdown.",
                    FetchException.Code.CLOSED)
        }
    }

    override fun getNewFileDownloaderForDownload(download: Download): FileDownloader {
        return FileDownloaderImpl(
                initialDownload = download,
                downloader = downloader,
                progressReportingIntervalMillis = progressReportingIntervalMillis,
                downloadBufferSizeBytes = downloadBufferSizeBytes,
                logger = logger,
                networkInfoProvider = networkInfoProvider,
                retryOnNetworkGain = retryOnNetworkGain)
    }

    override fun getFileDownloaderDelegate(): FileDownloader.Delegate {
        return FileDownloaderDelegate(
                downloadInfoUpdater = downloadInfoUpdater,
                uiHandler = uiHandler,
                fetchListener = fetchListenerProvider.mainListener,
                logger = logger,
                retryOnNetworkGain = retryOnNetworkGain)
    }

}