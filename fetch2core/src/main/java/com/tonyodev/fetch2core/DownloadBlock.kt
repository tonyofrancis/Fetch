package com.tonyodev.fetch2core

interface DownloadBlock {

    val downloadId: Int

    val blockPosition: Int

    val startByte: Long

    val endByte: Long

    val downloadedBytes: Long

    val progress: Int

    fun copy(): DownloadBlock

}