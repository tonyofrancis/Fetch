package com.tonyodev.fetch2.downloader

import android.content.Context
import android.content.Intent
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.helper.DownloadInfoUpdater
import com.tonyodev.fetch2.helper.FileDownloaderDelegate
import com.tonyodev.fetch2.fetch.ListenerCoordinator
import com.tonyodev.fetch2.provider.GroupInfoProvider
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.getRequestForDownload
import com.tonyodev.fetch2core.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DownloadManagerImpl(private val httpDownloader: Downloader<*, *>,
                          concurrentLimit: Int,
                          private val progressReportingIntervalMillis: Long,
                          private val logger: Logger,
                          private val networkInfoProvider: NetworkInfoProvider,
                          private val retryOnNetworkGain: Boolean,
                          private val downloadInfoUpdater: DownloadInfoUpdater,
                          private val downloadManagerCoordinator: DownloadManagerCoordinator,
                          private val listenerCoordinator: ListenerCoordinator,
                          private val fileServerDownloader: FileServerDownloader,
                          private val hashCheckingEnabled: Boolean,
                          private val storageResolver: StorageResolver,
                          private val context: Context,
                          private val namespace: String,
                          private val groupInfoProvider: GroupInfoProvider,
                          private val globalAutoRetryMaxAttempts: Int,
                          private val preAllocateFileOnCreation: Boolean) : DownloadManager {

    private val lock = Any()
    private var executor: ExecutorService? = getNewDownloadExecutorService(concurrentLimit)
    @Volatile
    override var concurrentLimit: Int = concurrentLimit
        set(value) {
            synchronized(lock) {
                try {
                    getActiveDownloadsIds().forEach { id ->
                        cancelDownloadNoLock(id)
                    }
                } catch (e: Exception) {
                }
                try {
                    executor?.shutdown()
                } catch (e: Exception) {
                }
                executor = getNewDownloadExecutorService(value)
                field = value
                logger.d("DownloadManager concurrentLimit changed from $field to $value")
            }
        }
    private val currentDownloadsMap = hashMapOf<Int, FileDownloader?>()
    @Volatile
    private var downloadCounter = 0
    @Volatile
    private var closed = false
    override val isClosed: Boolean
        get() {
            return closed
        }

    override fun start(download: Download): Boolean {
        return synchronized(lock) {
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
            downloadCounter += 1
            currentDownloadsMap[download.id] = null
            downloadManagerCoordinator.addFileDownloader(download.id, null)
            val downloadExecutor = executor
            if (downloadExecutor != null && !downloadExecutor.isShutdown) {
                downloadExecutor.execute {
                    try {
                        Thread.currentThread().name = "${download.namespace}-${download.id}"
                    } catch (e: Exception) {

                    }
                    try {
                        val fileDownloader = getNewFileDownloaderForDownload(download)
                        val runDownload = synchronized(lock) {
                            if (currentDownloadsMap.containsKey(download.id)) {
                                fileDownloader.delegate = getFileDownloaderDelegate()
                                currentDownloadsMap[download.id] = fileDownloader
                                downloadManagerCoordinator.addFileDownloader(download.id, fileDownloader)
                                logger.d("DownloadManager starting download $download")
                                true
                            } else {
                                false
                            }
                        }
                        if (runDownload) {
                            fileDownloader.run()
                        }
                        removeDownloadMappings(download)
                        groupInfoProvider.clean()
                    } catch (e: Exception) {
                        logger.e("DownloadManager failed to start download $download", e)
                    } finally {
                        removeDownloadMappings(download)
                        val intent = Intent(ACTION_QUEUE_BACKOFF_RESET)
                        intent.putExtra(EXTRA_NAMESPACE, namespace)
                        context.sendBroadcast(intent)
                    }
                }
                return true
            } else {
                false
            }
        }
    }

    private fun removeDownloadMappings(download: Download) {
        synchronized(lock) {
            if (currentDownloadsMap.containsKey(download.id)) {
                currentDownloadsMap.remove(download.id)
                downloadCounter -= 1
            }
            downloadManagerCoordinator.removeFileDownloader(download.id)
        }
    }

    override fun cancel(downloadId: Int): Boolean {
        return synchronized(lock) {
            cancelDownloadNoLock(downloadId)
        }
    }

    private fun cancelDownloadNoLock(downloadId: Int): Boolean {
        throwExceptionIfClosed()
        return if (currentDownloadsMap.containsKey(downloadId)) {
            val fileDownloader = currentDownloadsMap[downloadId]
            fileDownloader?.interrupted = true
            currentDownloadsMap.remove(downloadId)
            downloadCounter -= 1
            downloadManagerCoordinator.removeFileDownloader(downloadId)
            if (fileDownloader != null) {
                logger.d("DownloadManager cancelled download ${fileDownloader.download}")
            }
            true
        } else {
            downloadManagerCoordinator.interruptDownload(downloadId)
            false
        }
    }

    override fun cancelAll() {
        synchronized(lock) {
            throwExceptionIfClosed()
            cancelAllDownloads()
        }
    }

    private fun cancelAllDownloads() {
        if (concurrentLimit > 0) {
            downloadManagerCoordinator.getFileDownloaderList()
                    .iterator()
                    .forEach {
                        val fileDownloader = it
                        if (fileDownloader != null) {
                            fileDownloader.interrupted = true
                            downloadManagerCoordinator.removeFileDownloader(fileDownloader.download.id)
                            logger.d("DownloadManager cancelled download ${fileDownloader.download}")
                        }
                    }
        }
        currentDownloadsMap.clear()
        downloadCounter = 0
    }

    private fun terminateAllDownloads() {
        currentDownloadsMap.iterator()
                .forEach {
                    val fileDownloader = it.value
                    if (fileDownloader != null) {
                        fileDownloader.terminated = true
                        logger.d("DownloadManager terminated download ${fileDownloader.download}")
                        downloadManagerCoordinator.removeFileDownloader(it.key)
                    }
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
            if (concurrentLimit > 0) {
                terminateAllDownloads()
            }
            logger.d("DownloadManager closing download manager")
            try {
                executor?.shutdown()
            } catch (e: Exception) {
            }
        }
    }

    override fun contains(downloadId: Int): Boolean {
        synchronized(lock) {
            return !isClosed && downloadManagerCoordinator.containsFileDownloader(downloadId)
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

    override fun getActiveDownloads(): List<Download> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return currentDownloadsMap.values.filterNotNull().map { it.download }
        }
    }

    override fun getActiveDownloadsIds(): List<Int> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return currentDownloadsMap.filter { it.value != null }.map { it.key }
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchException("DownloadManager is already shutdown.")
        }
    }

    override fun getNewFileDownloaderForDownload(download: Download): FileDownloader {
        return if (!isFetchFileServerUrl(download.url)) {
            getFileDownloader(download, httpDownloader)
        } else {
            getFileDownloader(download, fileServerDownloader)
        }
    }

    private fun getFileDownloader(download: Download, downloader: Downloader<*, *>): FileDownloader {
        val request = getRequestForDownload(download)
        val supportedDownloadTypes = downloader.getRequestSupportedFileDownloaderTypes(request)
        return if (downloader.getRequestFileDownloaderType(request, supportedDownloadTypes) == Downloader.FileDownloaderType.SEQUENTIAL) {
            SequentialFileDownloaderImpl(
                    initialDownload = download,
                    downloader = downloader,
                    progressReportingIntervalMillis = progressReportingIntervalMillis,
                    logger = logger,
                    networkInfoProvider = networkInfoProvider,
                    retryOnNetworkGain = retryOnNetworkGain,
                    hashCheckingEnabled = hashCheckingEnabled,
                    storageResolver = storageResolver,
                    preAllocateFileOnCreation = preAllocateFileOnCreation)
        } else {
            ParallelFileDownloaderImpl(
                    initialDownload = download,
                    downloader = downloader,
                    progressReportingIntervalMillis = progressReportingIntervalMillis,
                    logger = logger,
                    networkInfoProvider = networkInfoProvider,
                    retryOnNetworkGain = retryOnNetworkGain,
                    fileTempDir = storageResolver.getDirectoryForFileDownloaderTypeParallel(request),
                    hashCheckingEnabled = hashCheckingEnabled,
                    storageResolver = storageResolver,
                    preAllocateFileOnCreation = preAllocateFileOnCreation)
        }
    }

    override fun getFileDownloaderDelegate(): FileDownloader.Delegate {
        return FileDownloaderDelegate(
                downloadInfoUpdater = downloadInfoUpdater,
                fetchListener = listenerCoordinator.mainListener,
                retryOnNetworkGain = retryOnNetworkGain,
                globalAutoRetryMaxAttempts = globalAutoRetryMaxAttempts)
    }

    override fun getDownloadFileTempDir(download: Download): String {
        val request = getRequestForDownload(download)
        return storageResolver.getDirectoryForFileDownloaderTypeParallel(request)
    }

    private fun getNewDownloadExecutorService(concurrentLimit: Int): ExecutorService? {
        return if (concurrentLimit > 0) {
            Executors.newFixedThreadPool(concurrentLimit)
        } else {
            null
        }
    }

}