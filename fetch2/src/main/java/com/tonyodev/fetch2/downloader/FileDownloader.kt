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
                      etaInMilliseconds: Long)

        fun onProgress(download: Download,
                       etaInMilliSeconds: Long)

        fun onError(download: Download)

        fun onComplete(download: Download)
    }

}