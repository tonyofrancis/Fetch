package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.database.DownloadInfo


abstract class DownloadReportingRunnable : Runnable {

    var download: Download = DownloadInfo()

    var etaInMilliSeconds = 0L

    var downloadedBytesPerSecond = 0L

}