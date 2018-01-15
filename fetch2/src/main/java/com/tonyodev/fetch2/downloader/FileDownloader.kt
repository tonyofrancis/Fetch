package com.tonyodev.fetch2.downloader

import com.tonyodev.fetch2.Download

interface FileDownloader : Runnable {

    var interrupted: Boolean
    var terminated: Boolean
    var completedDownload: Boolean
    var delegate: Delegate?
    val download: Download

    interface Delegate {

        fun onStarted(download: Download,
                      etaInMilliseconds: Long,
                      downloadedBytesPerSecond: Long)

        fun onProgress(download: Download,
                       etaInMilliSeconds: Long,
                       downloadedBytesPerSecond: Long)

        fun onError(download: Download)

        fun onComplete(download: Download)
    }

}