package com.tonyodev.fetch2.downloader

import android.os.Environment
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Downloader

interface FileDownloader : Runnable {

    var interrupted: Boolean
    var terminated: Boolean
    val completedDownload: Boolean
    var delegate: Delegate?
    val download: Download

    interface Delegate {

        val interrupted: Boolean

        fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int)

        fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int)

        fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long)

        fun onError(download: Download, error: Error, throwable: Throwable?)

        fun onComplete(download: Download)

        fun saveDownloadProgress(download: Download)

        fun getNewDownloadInfoInstance(): DownloadInfo

    }

}

abstract class AbsFileDownloader(
    protected val initialDownload: Download,
    protected val reservedStorageSize: Long
) : FileDownloader {

    fun checkDownloadSpaceSafely(response: Downloader.Response): Boolean {
        return response.contentLength.let { len ->
            if (len < 0) {
                return@let true
            }
            initialDownload.file.let {
                val spaceLeft = when {
                    it.startsWith("/data/data") -> {
                        Environment.getDataDirectory().freeSpace
                    }
                    Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED -> {
                        Environment.getExternalStorageDirectory().freeSpace
                    }
                    else -> 0L
                }
                spaceLeft > len.plus(reservedStorageSize)
            }
        }.let {
            if (!it) {
                throw IllegalStateException("No space left on device.")
            }
            true
        }
    }

}
