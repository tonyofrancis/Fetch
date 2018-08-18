package com.tonyodev.fetch2.helper

import android.os.Handler
import android.os.Looper
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2.downloader.FileDownloader
import com.tonyodev.fetch2.util.defaultNoError
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Logger


class FileDownloaderDelegate(private val downloadInfoUpdater: DownloadInfoUpdater,
                             private val fetchListener: FetchListener,
                             private val logger: Logger,
                             private val retryOnNetworkGain: Boolean) : FileDownloader.Delegate {

    private val uiHandler = Handler(Looper.getMainLooper())
    private val lock = Any()
    @Volatile
    override var interrupted = false
        set(value) {
            synchronized(lock) {
                uiHandler.removeCallbacksAndMessages(null)
                field = value
            }
        }

    override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
        synchronized(lock) {
            if (!interrupted) {
                val downloadInfo = download as DownloadInfo
                downloadInfo.status = Status.DOWNLOADING
                try {
                    downloadInfoUpdater.update(downloadInfo)
                    uiHandler.post {
                        fetchListener.onStarted(download, downloadBlocks, totalBlocks)
                    }
                } catch (e: Exception) {
                    logger.e("DownloadManagerDelegate", e)
                }
            }
        }
    }

    private val progressRunnable = object : DownloadReportingRunnable() {
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

    private val downloadBlockProgressRunnable = object : DownloadBlockReportingRunnable() {
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

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        synchronized(lock) {
            if (!interrupted) {
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
                            fetchListener.onError(downloadInfo, error, throwable)
                        }
                    }
                } catch (e: Exception) {
                    logger.e("DownloadManagerDelegate", e)
                }
            }
        }
    }

    override fun onComplete(download: Download) {
        synchronized(lock) {
            if (!interrupted) {
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
        }
    }

    override fun saveDownloadProgress(download: Download) {
        synchronized(lock) {
            if (!interrupted) {
                try {
                    val downloadInfo = download as DownloadInfo
                    downloadInfo.status = Status.DOWNLOADING
                    downloadInfoUpdater.updateFileBytesInfoAndStatusOnly(downloadInfo)
                } catch (e: Exception) {
                    logger.e("DownloadManagerDelegate", e)
                }
            }
        }
    }

}