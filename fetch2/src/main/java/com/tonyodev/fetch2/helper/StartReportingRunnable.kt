package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2core.DownloadBlock

abstract class StartReportingRunnable : Runnable {

    var download: Download = DownloadInfo()

    var downloadBlocks: List<DownloadBlock> = listOf()

    var totalBlocks = 0

}