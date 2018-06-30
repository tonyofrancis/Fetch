@file:JvmName("FetchUtils")

package com.tonyodev.fetch2.util

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.getFile


fun canPauseDownload(download: Download): Boolean {
    return when (download.status) {
        Status.DOWNLOADING,
        Status.QUEUED -> true
        else -> false
    }
}

fun canResumeDownload(download: Download): Boolean {
    return when (download.status) {
        Status.NONE,
        Status.PAUSED -> true
        else -> false
    }
}

fun canRetryDownload(download: Download): Boolean {
    return when (download.status) {
        Status.NONE,
        Status.FAILED,
        Status.CANCELLED -> true
        else -> false
    }
}

fun canCancelDownload(download: Download): Boolean {
    return when (download.status) {
        Status.COMPLETED,
        Status.FAILED -> false
        else -> true
    }
}

fun getRequestForDownload(download: Download,
                          rangeStart: Long = -1,
                          rangeEnd: Long = -1): Downloader.ServerRequest {
    val start = if (rangeStart == -1L) 0 else rangeStart
    val end = if (rangeEnd == -1L) "" else rangeEnd.toString()
    val headers = download.headers.toMutableMap()
    headers["Range"] = "bytes=$start-$end"
    return Downloader.ServerRequest(
            id = download.id,
            url = download.url,
            headers = headers,
            file = download.file,
            tag = download.tag,
            identifier = download.identifier)
}

fun deleteRequestTempFiles(fileTempDir: String,
                           downloader: Downloader,
                           download: Download) {
    try {
        val request = getRequestForDownload(download)
        val tempDirPath = downloader.getDirectoryForFileDownloaderTypeParallel(request)
                ?: fileTempDir
        val tempDir = getFile(tempDirPath)
        if (tempDir.exists()) {
            val tempFiles = tempDir.listFiles()
            for (tempFile in tempFiles) {
                val match = tempFile.name.startsWith("${download.id}.")
                if (match && tempFile.exists()) {
                    try {
                        tempFile.delete()
                    } catch (e: Exception) {
                    }
                }
            }
        }
    } catch (e: Exception) {

    }
}

fun createConfigWithNewNamespace(fetchConfiguration: FetchConfiguration,
                                 namespace: String): FetchConfiguration {
    return FetchConfiguration.Builder(fetchConfiguration.appContext)
            .setNamespace(namespace)
            .enableAutoStart(fetchConfiguration.autoStart)
            .enableLogging(fetchConfiguration.loggingEnabled)
            .enableRetryOnNetworkGain(fetchConfiguration.retryOnNetworkGain)
            .setDownloadBufferSize(fetchConfiguration.downloadBufferSizeBytes)
            .setHttpDownloader(fetchConfiguration.httpDownloader)
            .setDownloadConcurrentLimit(fetchConfiguration.concurrentLimit)
            .setProgressReportingInterval(fetchConfiguration.progressReportingIntervalMillis)
            .setGlobalNetworkType(fetchConfiguration.globalNetworkType)
            .setLogger(fetchConfiguration.logger)
            .build()
}