@file:JvmName("FetchUtils")

package com.tonyodev.fetch2.util

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.*
import kotlin.math.ceil

fun canPauseDownload(download: Download): Boolean {
    return when (download.status) {
        Status.DOWNLOADING,
        Status.QUEUED -> true
        else -> false
    }
}

fun canResumeDownload(download: Download): Boolean {
    return when (download.status) {
        Status.ADDED,
        Status.PAUSED -> true
        else -> false
    }
}

fun canRetryDownload(download: Download): Boolean {
    return when (download.status) {
        Status.ADDED,
        Status.FAILED,
        Status.CANCELLED -> true
        else -> false
    }
}

fun canCancelDownload(download: Download): Boolean {
    return when (download.status) {
        Status.COMPLETED,
        Status.NONE,
        Status.FAILED -> false
        else -> true
    }
}

fun getRequestForDownload(download: Download, requestMethod: String = GET_REQUEST_METHOD): Downloader.ServerRequest {
    return getRequestForDownload(download, -1, -1, requestMethod)
}

fun getRequestForDownload(download: Download,
                          rangeStart: Long = -1,
                          rangeEnd: Long = -1,
                          requestMethod: String = GET_REQUEST_METHOD): Downloader.ServerRequest {
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
            identifier = download.identifier,
            requestMethod = requestMethod)
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

fun getPreviousSliceCount(id: Int, fileTempDir: String): Int {
    var sliceCount = -1
    try {
        sliceCount = getSingleLineTextFromFile(getMetaFilePath(id, fileTempDir))?.toInt() ?: -1
    } catch (e: Exception) {

    }
    return sliceCount
}

fun getMetaFilePath(id: Int, fileTempDir: String): String {
    return "$fileTempDir/$id.meta.txt"
}

fun saveCurrentSliceCount(id: Int, SliceCount: Int, fileTempDir: String) {
    try {
        writeTextToFile(getMetaFilePath(id, fileTempDir), SliceCount.toString())
    } catch (e: Exception) {

    }
}

fun getDownloadedInfoFilePath(id: Int, position: Int, fileTempDir: String): String {
    return "$fileTempDir/$id.$position.txt"
}

fun deleteTempFile(id: Int, position: Int, fileTempDir: String) {
    try {
        val textFile = getFile(getDownloadedInfoFilePath(id, position, fileTempDir))
        if (textFile.exists()) {
            textFile.delete()
        }
    } catch (e: Exception) {

    }
}

fun deleteMetaFile(id: Int, fileTempDir: String) {
    try {
        val textFile = getFile(getMetaFilePath(id, fileTempDir))
        if (textFile.exists()) {
            textFile.delete()
        }
    } catch (e: Exception) {

    }
}

fun getSavedDownloadedInfo(id: Int, position: Int, fileTempDir: String): Long {
    var downloaded = 0L
    try {
        downloaded = getSingleLineTextFromFile(getDownloadedInfoFilePath(id, position, fileTempDir))?.toLong() ?: 0L
    } catch (e: Exception) {

    }
    return downloaded
}

fun saveDownloadedInfo(id: Int, position: Int, downloaded: Long, fileTempDir: String) {
    try {
        writeTextToFile(getDownloadedInfoFilePath(id, position, fileTempDir), downloaded.toString())
    } catch (e: Exception) {

    }
}

fun getFileSliceInfo(fileSliceSize: Int, totalBytes: Long): FileSliceInfo {
    return if (fileSliceSize == DEFAULT_FILE_SLICE_NO_LIMIT_SET) {
        val fileSizeInMb = totalBytes.toFloat() / 1024F * 1024F
        val fileSizeInGb = totalBytes.toFloat() / 1024F * 1024F * 1024F
        when {
            fileSizeInGb >= 1F -> {
                val slices = 6
                val bytesPerSlice = ceil((totalBytes.toFloat() / slices.toFloat())).toLong()
                FileSliceInfo(slices, bytesPerSlice)
            }
            fileSizeInMb >= 1F -> {
                val slices = 4
                val bytesPerSlice = ceil((totalBytes.toFloat() / slices.toFloat())).toLong()
                FileSliceInfo(slices, bytesPerSlice)
            }
            else -> FileSliceInfo(2, totalBytes)
        }
    } else {
        val bytesPerSlice = ceil((totalBytes.toFloat() / fileSliceSize.toFloat())).toLong()
        return FileSliceInfo(fileSliceSize, bytesPerSlice)
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