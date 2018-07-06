package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.DownloadBlockInfo

abstract class DownloadBlockReportingRunnable : Runnable {

    @Volatile
    var download: Download = DownloadInfo()

    @Volatile
    var downloadBlock: DownloadBlock = DownloadBlockInfo()

    @Volatile
    var totalBlocks: Int = -1

}