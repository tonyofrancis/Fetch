package com.tonyodev.fetch2.helper

import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2.downloader.FileDownloader
import com.tonyodev.fetch2.util.defaultNoError
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.HandlerWrapper
import com.tonyodev.fetch2core.Logger


class FileDownloaderDelegate(private val downloadInfoUpdater: DownloadInfoUpdater,
                             private val uiHandler: Handler,
                             private val fetchListener: FetchListener,
                             private val logger: Logger,
                             private val retryOnNetworkGain: Boolean,
                             private val downloadBlockHandlerWrapper: HandlerWrapper) : FileDownloader.Delegate {

    override fun onStarted(download: Download, etaInMilliseconds: Long, downloadedBytesPerSecond: Long) {
        val downloadInfo = download as DownloadInfo
        downloadInfo.status = Status.DOWNLOADING
        try {
            downloadInfoUpdater.update(downloadInfo)
            uiHandler.post {
                fetchListener.onProgress(downloadInfo, etaInMilliseconds, downloadedBytesPerSecond)
            }
        } catch (e: Exception) {
            logger.e("DownloadManagerDelegate", e)
        }
    }

    private val progressRunnable = object : DownloadReportingRunnable() {
        override fun run() {
            fetchListener.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
        }
    }

    override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
        try {
            progressRunnable.download = download
            progressRunnable.etaInMilliSeconds = etaInMilliSeconds
            progressRunnable.downloadedBytesPerSecond = downloadedBytesPerSecond
            uiHandler.post(progressRunnable)
        } catch (e: Exception) {
            logger.e("DownloadManagerDelegate", e)
        }
    }

    private val downloadBlockProgressRunnable = object : DownloadBlockReportingRunnable() {
        override fun run() {
            try {
                fetchListener.onDownloadBlockUpdated(download, downloadBlock, totalBlocks)
            } catch (e: Exception) {
                logger.e("DownloadManagerDelegate", e)
            }
        }
    }

    override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {
        downloadBlockProgressRunnable.download = download
        downloadBlockProgressRunnable.downloadBlock = downloadBlock
        downloadBlockProgressRunnable.totalBlocks = totalBlocks
        downloadBlockHandlerWrapper.post(downloadBlockProgressRunnable)
    }

    override fun onError(download: Download) {
        val downloadInfo = download as DownloadInfo
        try {
            if (retryOnNetworkGain && downloadInfo.error == Error.NO_NETWORK_CONNECTION) {
                downloadInfo.status = Status.QUEUED
                downloadInfo.error = defaultNoError
                downloadInfoUpdater.update(downloadInfo)
                uiHandler.post {
                    fetchListener.onQueued(downloadInfo, true)
                }
            } else {
                downloadInfo.status = Status.FAILED
                downloadInfoUpdater.update(downloadInfo)
                uiHandler.post {
                    fetchListener.onError(downloadInfo)
                }
            }
        } catch (e: Exception) {
            logger.e("DownloadManagerDelegate", e)
        }
    }

    override fun onComplete(download: Download) {
        val downloadInfo = download as DownloadInfo
        downloadInfo.status = Status.COMPLETED
        try {
            downloadInfoUpdater.update(downloadInfo)
            uiHandler.post {
                fetchListener.onCompleted(downloadInfo)
            }
        } catch (e: Exception) {
            logger.e("DownloadManagerDelegate", e)
        }
    }

    override fun saveDownloadProgress(download: Download) {
        try {
            val downloadInfo = download as DownloadInfo
            downloadInfo.status = Status.DOWNLOADING
            downloadInfoUpdater.updateFileBytesInfoAndStatusOnly(downloadInfo)
        } catch (e: Exception) {
            logger.e("DownloadManagerDelegate", e)
        }
    }

}