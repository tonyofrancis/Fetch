package com.tonyodev.fetch2.downloader

import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.exception.FetchImplementationException
import com.tonyodev.fetch2.helper.DownloadInfoUpdater
import com.tonyodev.fetch2.helper.FileDownloaderDelegate
import com.tonyodev.fetch2.provider.ListenerProvider
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.getRequestForDownload
import java.util.concurrent.Executors

class DownloadManagerImpl(private val downloader: Downloader,
                          private val concurrentLimit: Int,
                          private val progressReportingIntervalMillis: Long,
                          private val downloadBufferSizeBytes: Int,
                          private val logger: Logger,
                          private val networkInfoProvider: NetworkInfoProvider,
                          private val retryOnNetworkGain: Boolean,
                          private val uiHandler: Handler,
                          private val downloadInfoUpdater: DownloadInfoUpdater,
                          private val fileTempDir: String,
                          private val namespace: String) : DownloadManager {

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
            addFileDownloaderToRegistry(namespace, download.id, fileDownloader)
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
                        removeFileDownloaderFromRegistry(namespace, download.id)
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
                removeFileDownloaderFromRegistry(namespace, id)
                logger.d("DownloadManager cancelled download ${fileDownloader.download}")
                true
            } else {
                interruptDownloadInRegistry(namespace, id)
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
        getFileDownloaderListForNamespace(namespace).iterator().forEach {
            it.interrupted = true
            while (!it.terminated) {
                //Wait until download runnable terminates
            }
            removeFileDownloaderFromRegistry(namespace, it.download.id)
            logger.d("DownloadManager cancelled download ${it.download}")
        }
        currentDownloadsMap.clear()
        downloadCounter = 0
    }

    private fun terminateAllDownloads() {
        throwExceptionIfClosed()
        currentDownloadsMap.iterator().forEach {
            it.value.terminated = true
            while (!it.value.terminated) {
                //Wait until download runnable terminates
            }
            logger.d("DownloadManager terminated download ${it.value.download}")
            removeFileDownloaderFromRegistry(namespace, it.key)
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
            return !isClosed && registryContainsFileDownloader(namespace, id)
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
        return if (downloader.getFileDownloaderType(request) == Downloader.FileDownloaderType.SEQUENTIAL) {
            SequentialFileDownloaderImpl(
                    initialDownload = download,
                    downloader = downloader,
                    progressReportingIntervalMillis = progressReportingIntervalMillis,
                    downloadBufferSizeBytes = downloadBufferSizeBytes,
                    logger = logger,
                    networkInfoProvider = networkInfoProvider,
                    retryOnNetworkGain = retryOnNetworkGain)
        } else {
            val tempDir = downloader.getDirectoryForFileDownloaderTypeParallel(request)
                    ?: fileTempDir
            ParallelFileDownloaderImpl(
                    initialDownload = download,
                    downloader = downloader,
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
                fetchListener = ListenerProvider.mainListener,
                logger = logger,
                retryOnNetworkGain = retryOnNetworkGain)
    }

    companion object {

        private val fileDownloaderMap = mutableMapOf<String, MutableMap<Int, FileDownloader>>()
        private val lock = Any()

        fun interruptDownloadInRegistry(namespace: String, downloadId: Int) {
            synchronized(lock) {
                val map = fileDownloaderMap[namespace]
                if (map != null) {
                    val fileDownloader = map[downloadId]
                    if (fileDownloader != null) {
                        fileDownloader.interrupted = true
                        while (!fileDownloader.terminated) {
                            //Wait until download runnable terminates
                        }
                        map.remove(downloadId)
                    }
                }
            }
        }

        fun addFileDownloaderToRegistry(namespace: String, downloadId: Int, fileDownloader: FileDownloader) {
            synchronized(lock) {
                val map = fileDownloaderMap[namespace] ?: mutableMapOf()
                fileDownloaderMap[namespace] = map
                map[downloadId] = fileDownloader
            }
        }

        fun removeFileDownloaderFromRegistry(namespace: String, downloadId: Int) {
            synchronized(lock) {
                val map = fileDownloaderMap[namespace]
                map?.remove(downloadId)
            }
        }

        fun getFileDownloaderListForNamespace(namespace: String): List<FileDownloader> {
            return synchronized(lock) {
                fileDownloaderMap[namespace]?.values?.toList() ?: listOf()
            }
        }

        fun registryContainsFileDownloader(namespace: String, downloadId: Int): Boolean {
            return synchronized(lock) {
                val map = fileDownloaderMap[namespace]
                val fileDownloader = map?.get(downloadId)
                fileDownloader != null
            }
        }

    }

}