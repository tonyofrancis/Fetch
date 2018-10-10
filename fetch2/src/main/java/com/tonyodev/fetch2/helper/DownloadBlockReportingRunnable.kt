package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.DownloadBlockInfo

abstract class DownloadBlockReportingRunnable : Runnable {

    var download: Download = DownloadInfo()

    var downloadBlock: DownloadBlock = DownloadBlockInfo()

    var totalBlocks: Int = -1

}