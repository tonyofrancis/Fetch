package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Downloader
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.exception.FetchImplementationException
import java.util.concurrent.Executors

open class DownloadManagerImpl(val downloader: Downloader,
                               val concurrentLimit: Int,
                               val progressReportingIntervalMillis: Long,
                               val downloadBufferSizeBytes: Int,
                               val logger: Logger) : DownloadManager {

    val lock = Object()
    open val executorInternal = Executors.newFixedThreadPool(concurrentLimit)
    open val currentDownloadsMapInternal = hashMapOf<Int, FileDownloader>()
    override var delegate: DownloadManager.Delegate? = null
    @Volatile
    private var downloadCounter = 0
    @Volatile
    private var closed = false
    override val isClosed: Boolean
        get() = closed

    override fun start(download: Download): Boolean {
        synchronized(lock) {
            throwExceptionIfClosed()
            if (currentDownloadsMapInternal.containsKey(download.id)) {
                logger.d("DownloadManager already running download $download")
                return false
            }
            if (downloadCounter >= concurrentLimit) {
                logger.d("DownloadManager cannot init download $download because " +
                        "the download queue is full")
                return false
            }
            val fileDownloader = getNewFileDownloaderForDownload(download)
            fileDownloader.delegate = delegate
            downloadCounter += 1
            currentDownloadsMapInternal[download.id] = fileDownloader
            return try {
                executorInternal.execute {
                    logger.d("DownloadManager starting download $download")
                    fileDownloader.run()
                    synchronized(lock) {
                        if (currentDownloadsMapInternal.containsKey(download.id)) {
                            currentDownloadsMapInternal.remove(download.id)
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
            return if (currentDownloadsMapInternal.containsKey(id)) {
                val fileDownloader = currentDownloadsMapInternal[id] as FileDownloader
                fileDownloader.interrupted = true
                while (!fileDownloader.terminated) {
                    //Wait until download runnable terminates
                }
                currentDownloadsMapInternal.remove(id)
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
            cancelAllDownloadsInternal()
        }
    }

    open fun cancelAllDownloadsInternal() {
        currentDownloadsMapInternal.iterator().forEach {
            it.value.interrupted = true
            while (!it.value.terminated) {
                //Wait until download runnable terminates
            }
            logger.d("DownloadManager cancelled download ${it.value.download}")
        }
        currentDownloadsMapInternal.clear()
        downloadCounter = 0
    }

    override fun close() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
            logger.d("DownloadManager closing download manager")
            cancelAllDownloadsInternal()
            executorInternal.shutdown()
            downloader.close()
        }
    }

    override fun contains(id: Int): Boolean {
        synchronized(lock) {
            throwExceptionIfClosed()
            return currentDownloadsMapInternal.containsKey(id)
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
            return currentDownloadsMapInternal.values.map { it.download }
        }
    }

    open fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchImplementationException("DownloadManager is already shutdown.",
                    FetchImplementationException.Code.CLOSED)
        }
    }

    override fun getNewFileDownloaderForDownload(download: Download): FileDownloader {
        return FileDownloaderImpl(
                initialDownload = download,
                downloader = downloader,
                progressReportingIntervalMillis = progressReportingIntervalMillis,
                downloadBufferSizeBytes = downloadBufferSizeBytes,
                logger = logger)
    }

}