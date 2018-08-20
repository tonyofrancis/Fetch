package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.database.DownloadInfo

abstract class CompletedReportingRunnable : Runnable {

    var download: Download = DownloadInfo()

}