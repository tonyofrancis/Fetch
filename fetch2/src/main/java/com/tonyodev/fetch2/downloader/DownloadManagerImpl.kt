package com.tonyodev.fetch2.downloader

import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.exception.FetchImplementationException
import com.tonyodev.fetch2.fetch.DownloadManagerCoordinator
import com.tonyodev.fetch2.helper.DownloadInfoUpdater
import com.tonyodev.fetch2.helper.FileDownloaderDelegate
import com.tonyodev.fetch2.fetch.ListenerCoordinator
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.getRequestForDownload
import java.util.concurrent.Executors

class DownloadManagerImpl(private val httpDownloader: Downloader,
                          private val concurrentLimit: Int,
                          private val progressReportingIntervalMillis: Long,
                          private val downloadBufferSizeBytes: Int,
                          private val logger: Logger,
                          private val networkInfoProvider: NetworkInfoProvider,
                          private val retryOnNetworkGain: Boolean,
                          private val uiHandler: Handler,
                          private val downloadInfoUpdater: DownloadInfoUpdater,
                          private val fileTempDir: String,
                          private val downloadManagerCoordinator: DownloadManagerCoordinator,
                          private val listenerCoordinator: ListenerCoordinator) : DownloadManager {

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
            downloadManagerCoordinator.addFileDownloader(download.id, fileDownloader)
            return try {
                val downloadInfo = download as DownloadInfo
                downloadInfo.status = Status.DOWNLOADING
                downloadInfoUpdater.update(downloadInfo)
                executor.execute {
                    logger.d("DownloadManager starting download $download")
                    fileDownloader.run()
                    synchronized(lock) {
                        if (currentDownloadsMap.containsKey(download.id)) {
                            currentDownloadsMap.remove(download.id)
                            downloadCounter -= 1
                        }
                        downloadManagerCoordinator.removeFileDownloader(download.id)
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
                downloadManagerCoordinator.removeFileDownloader(id)
                logger.d("DownloadManager cancelled download ${fileDownloader.download}")
                true
            } else {
                downloadManagerCoordinator.interruptDownload(id)
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
        downloadManagerCoordinator.getFileDownloaderList().iterator().forEach {
            it.interrupted = true
            while (!it.terminated) {
                //Wait until download runnable terminates
            }
            downloadManagerCoordinator.removeFileDownloader(it.download.id)
            logger.d("DownloadManager cancelled download ${it.download}")
        }
        currentDownloadsMap.clear()
        downloadCounter = 0
    }

    private fun terminateAllDownloads() {
        currentDownloadsMap.iterator().forEach {
            it.value.terminated = true
            while (!it.value.terminated) {
                //Wait until download runnable terminates
            }
            logger.d("DownloadManager terminated download ${it.value.download}")
            downloadManagerCoordinator.removeFileDownloader(it.key)
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
            terminateAllDownloads()
            logger.d("DownloadManager closing download manager")
            executor.shutdown()
        }
    }

    override fun contains(id: Int): Boolean {
        synchronized(lock) {
            return !isClosed && downloadManagerCoordinator.containsFileDownloader(id)
        }
    }

    override fun canAccommodateNewDownload(): Boolean {
        synchronized(lock) {
            return !closed && downloadCounter < concurrentLimit
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
        val request = getRequestForDownload(download)
        return if (httpDownloader.getFileDownloaderType(request) == Downloader.FileDownloaderType.SEQUENTIAL) {
            SequentialFileDownloaderImpl(
                    initialDownload = download,
                    downloader = httpDownloader,
                    progressReportingIntervalMillis = progressReportingIntervalMillis,
                    downloadBufferSizeBytes = downloadBufferSizeBytes,
                    logger = logger,
                    networkInfoProvider = networkInfoProvider,
                    retryOnNetworkGain = retryOnNetworkGain)
        } else {
            val tempDir = httpDownloader.getDirectoryForFileDownloaderTypeParallel(request)
                    ?: fileTempDir
            ParallelFileDownloaderImpl(
                    initialDownload = download,
                    downloader = httpDownloader,
                    progressReportingIntervalMillis = progressReportingIntervalMillis,
                    downloadBufferSizeBytes = downloadBufferSizeBytes,
                    logger = logger,
                    networkInfoProvider = networkInfoProvider,
                    retryOnNetworkGain = retryOnNetworkGain,
                    fileTempDir = tempDir)
        }
    }

    override fun getFileDownloaderDelegate(): FileDownloader.Delegate {
        return FileDownloaderDelegate(
                downloadInfoUpdater = downloadInfoUpdater,
                uiHandler = uiHandler,
                fetchListener = listenerCoordinator.mainListener,
                logger = logger,
                retryOnNetworkGain = retryOnNetworkGain)
    }

    companion object {


    }

}