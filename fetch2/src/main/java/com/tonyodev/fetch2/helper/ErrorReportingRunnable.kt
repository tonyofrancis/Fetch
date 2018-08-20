package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2.util.defaultNoError

abstract class ErrorReportingRunnable : Runnable {

    var download: Download = DownloadInfo()

    var error: Error = defaultNoError

    var throwable: Throwable? = null
}