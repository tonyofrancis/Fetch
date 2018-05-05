package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.database.DownloadInfo


abstract class DownloadReportingRunnable: Runnable {

    @Volatile
    var download: Download = DownloadInfo()

    @Volatile
    var etaInMilliSeconds = 0L

    @Volatile
    var downloadedBytesPerSecond = 0L

}