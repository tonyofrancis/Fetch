package com.tonyodev.fetchmigrator.fetch1

import com.tonyodev.fetch2.Download

data class DownloadTransferPair(val newDownload: Download, val oldID: Long)