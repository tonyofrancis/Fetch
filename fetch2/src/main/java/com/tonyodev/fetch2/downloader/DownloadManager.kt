package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.Download
import java.io.Closeable

interface DownloadManager : Closeable {

    var delegate: Delegate?
    val isClosed: Boolean

    fun start(download: Download): Boolean
    fun cancel(id: Int): Boolean
    fun cancelAll()
    fun contains(id: Int): Boolean
    fun canAccommodateNewDownload(): Boolean
    fun getActiveDownloadCount(): Int
    fun getDownloads(): List<Download>
    fun getNewFileDownloaderForDownload(download: Download): FileDownloader

    interface Delegate : FileDownloader.Delegate {
        fun onDownloadRemovedFromManager(download: Download)
    }

}