package com.tonyodev.fetch2.helper

import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2.downloader.FileDownloader
import com.tonyodev.fetch2.util.defaultNoError
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.DownloadBlock


class FileDownloaderDelegate(private val downloadInfoUpdater: DownloadInfoUpdater,
                             private val fetchListener: FetchListener,
                             private val uiHandler: Handler,
                             private val retryOnNetworkGain: Boolean) : FileDownloader.Delegate {

    private val lock = Any()
    @Volatile
    override var interrupted = false
        set(value) {
            synchronized(lock) {
                uiHandler.removeCallbacks(startRunnable)
                uiHandler.removeCallbacks(progressRunnable)
                uiHandler.removeCallbacks(downloadBlockProgressRunnable)
                uiHandler.removeCallbacks(errorReportingRunnable)
                uiHandler.removeCallbacks(queuedReportingRunnable)
                field = value
            }
        }

    private val startRunnable: StartReportingRunnable = object : StartReportingRunnable() {
        override fun run() {
            fetchListener.onStarted(download, downloadBlocks, totalBlocks)
        }
    }

    override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
        synchronized(lock) {
            if (!interrupted) {
                val downloadInfo = download as DownloadInfo
                downloadInfo.status = Status.DOWNLOADING
                downloadInfoUpdater.update(downloadInfo)
                startRunnable.download = downloadInfo
                startRunnable.downloadBlocks = downloadBlocks
                startRunnable.totalBlocks = totalBlocks
                uiHandler.post(startRunnable)
            }
        }
    }

    private val progressRunnable: DownloadReportingRunnable = object : DownloadReportingRunnable() {
        override fun run() {
            fetchListener.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
        }
    }

    override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
        synchronized(lock) {
            if (!interrupted) {
                progressRunnable.download = download
                progressRunnable.etaInMilliSeconds = etaInMilliSeconds
                progressRunnable.downloadedBytesPerSecond = downloadedBytesPerSecond
                uiHandler.post(progressRunnable)
            }
        }
    }

    private val downloadBlockProgressRunnable: DownloadBlockReportingRunnable = object : DownloadBlockReportingRunnable() {
        override fun run() {
            fetchListener.onDownloadBlockUpdated(download, downloadBlock, totalBlocks)
        }
    }

    override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {
        synchronized(lock) {
            if (!interrupted) {
                downloadBlockProgressRunnable.download = download
                downloadBlockProgressRunnable.downloadBlock = downloadBlock
                downloadBlockProgressRunnable.totalBlocks = totalBlocks
                uiHandler.post(downloadBlockProgressRunnable)
            }
        }
    }

    private val errorReportingRunnable: ErrorReportingRunnable = object : ErrorReportingRunnable() {

        override fun run() {
            fetchListener.onError(download, error, throwable)
        }
    }

    private val queuedReportingRunnable: QueuedReportingRunnable = object : QueuedReportingRunnable() {

        override fun run() {
            fetchListener.onQueued(download, true)
        }

    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        synchronized(lock) {
            if (!interrupted) {
                val downloadInfo = download as DownloadInfo
                if (retryOnNetworkGain && downloadInfo.error == Error.NO_NETWORK_CONNECTION) {
                    downloadInfo.status = Status.QUEUED
                    downloadInfo.error = defaultNoError
                    downloadInfoUpdater.update(downloadInfo)
                    uiHandler.post(queuedReportingRunnable)
                } else {
                    downloadInfo.status = Status.FAILED
                    downloadInfoUpdater.update(downloadInfo)
                    uiHandler.post(errorReportingRunnable)
                }
            }
        }
    }

    override fun onComplete(download: Download) {
        synchronized(lock) {
            if (!interrupted) {
                val downloadInfo = download as DownloadInfo
                downloadInfo.status = Status.COMPLETED
                downloadInfoUpdater.update(downloadInfo)
                val completedReportingRunnable: CompletedReportingRunnable = object : CompletedReportingRunnable() {

                    override fun run() {
                        fetchListener.onCompleted(download)
                    }
                }
                uiHandler.post(completedReportingRunnable)
            }
        }
    }

    override fun saveDownloadProgress(download: Download) {
        synchronized(lock) {
            if (!interrupted) {
                val downloadInfo = download as DownloadInfo
                downloadInfo.status = Status.DOWNLOADING
                downloadInfoUpdater.updateFileBytesInfoAndStatusOnly(downloadInfo)
            }
        }
    }

}