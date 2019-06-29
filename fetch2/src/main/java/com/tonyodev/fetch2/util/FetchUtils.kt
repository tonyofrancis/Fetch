@file:JvmName("FetchUtils")

package com.tonyodev.fetch2.util

import android.os.Looper
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchHandler
import com.tonyodev.fetch2core.*
import com.tonyodev.fetch2core.server.FileRequest
import java.io.File
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
        Status.QUEUED,
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
                          requestMethod: String = GET_REQUEST_METHOD,
                          segment: Int = 1): Downloader.ServerRequest {
    val start = if (rangeStart == -1L) 0 else rangeStart
    val end = if (rangeEnd == -1L) "" else rangeEnd.toString()
    val headers = download.headers.toMutableMap()
    headers["Range"] = "bytes=$start-$end"
    return Downloader.ServerRequest(
            id = download.id,
            url = download.url,
            headers = headers,
            file = download.file,
            fileUri = getFileUri(download.file),
            tag = download.tag,
            identifier = download.identifier,
            requestMethod = requestMethod,
            extras = download.extras,
            redirected = false,
            redirectUrl = "",
            segment = segment)
}

fun getServerRequestFromRequest(request: Request): Downloader.ServerRequest {
    return Downloader.ServerRequest(
            id = request.id,
            url = request.url,
            headers = request.headers,
            tag = request.tag,
            identifier = request.identifier,
            requestMethod = GET_REQUEST_METHOD,
            file = request.file,
            fileUri = getFileUri(request.file),
            extras = request.extras,
            redirected = false,
            redirectUrl = "",
            segment = 1)
}

fun getCatalogServerRequestFromRequest(request: Request): Downloader.ServerRequest {
    val headers = request.headers.toMutableMap()
    headers["Range"] = "bytes=0-"
    headers[FileRequest.FIELD_PAGE] = "-1"
    headers[FileRequest.FIELD_SIZE] = "-1"
    headers[FileRequest.FIELD_TYPE] = FileRequest.TYPE_FILE.toString()
    return Downloader.ServerRequest(
            id = request.id,
            url = request.url,
            headers = headers,
            tag = request.tag,
            identifier = request.identifier,
            requestMethod = GET_REQUEST_METHOD,
            file = request.file,
            fileUri = getFileUri(request.file),
            extras = request.extras,
            redirected = false,
            redirectUrl = "",
            segment = 1)
}

fun getPreviousSliceCount(id: Int, fileTempDir: String): Int {
    var sliceCount = -1
    try {
        sliceCount = getLongDataFromFile(getMetaFilePath(id, fileTempDir))?.toInt() ?: -1
    } catch (e: Exception) {

    }
    return sliceCount
}

fun getMetaFilePath(id: Int, fileTempDir: String): String {
    return "$fileTempDir/$id.meta.data"
}

fun saveCurrentSliceCount(id: Int, SliceCount: Int, fileTempDir: String) {
    try {
        writeLongToFile(getMetaFilePath(id, fileTempDir), SliceCount.toLong())
    } catch (e: Exception) {

    }
}

fun getDownloadedInfoFilePath(id: Int, position: Int, fileTempDir: String): String {
    return "$fileTempDir/$id.$position.data"
}

fun deleteAllInFolderForId(id: Int, fileTempDir: String) {
    try {
        val dir = File(fileTempDir)
        if (dir.exists()) {
            val files = dir.listFiles()
            if (files != null) {
                val filteredFilesList = files.filter { file ->
                    file.nameWithoutExtension.startsWith("$id.")
                }
                filteredFilesList.forEach { file ->
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    } catch (e: Exception) {

    }
}

fun getSavedDownloadedInfo(id: Int, position: Int, fileTempDir: String): Long {
    var downloaded = 0L
    try {
        downloaded = getLongDataFromFile(getDownloadedInfoFilePath(id, position, fileTempDir)) ?: 0L
    } catch (e: Exception) {

    }
    return downloaded
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

fun awaitFinishOrTimeout(allowTimeInMilliseconds: Long, fetchHandler: FetchHandler) {
    if (Thread.currentThread() == Looper.getMainLooper().thread) {
        throw FetchException(AWAIT_CALL_ON_UI_THREAD)
    }
    var hasAllowedTimeExpired = false
    val indefinite = allowTimeInMilliseconds == 0L
    val sleepTime = when {
        indefinite -> 5000
        allowTimeInMilliseconds < 1000 -> allowTimeInMilliseconds
        else -> 1000
    }
    val timeStarted = System.currentTimeMillis()
    var pendingCount = fetchHandler.getPendingCount()
    while (indefinite || (pendingCount > 0 && !hasAllowedTimeExpired)) {
        try {
            Thread.sleep(sleepTime)
        } catch (e: Exception) {

        }
        hasAllowedTimeExpired = if (allowTimeInMilliseconds == -1L) {
            false
        } else {
            hasAllowedTimeExpired(timeStarted, System.currentTimeMillis(), allowTimeInMilliseconds)
        }
        pendingCount = fetchHandler.getPendingCount()
    }
}
