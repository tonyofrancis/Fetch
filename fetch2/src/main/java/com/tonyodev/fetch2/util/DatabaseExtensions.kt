@file:JvmName("FetchDatabaseExtensions")

package com.tonyodev.fetch2.util

import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.DatabaseManager
import com.tonyodev.fetch2.database.DownloadInfo
import java.io.File

@JvmOverloads
fun DatabaseManager.sanitize(initializing: Boolean = false): Boolean {
    return sanitize(get(), initializing)
}

@JvmOverloads
fun DatabaseManager.sanitize(downloads: List<DownloadInfo>, initializing: Boolean = false): Boolean {
    val changedDownloadsList = mutableListOf<DownloadInfo>()
    var file: File?
    var fileLength: Long
    var fileExist: Boolean
    var downloadInfo: DownloadInfo
    var update: Boolean
    for (i in 0 until downloads.size) {
        downloadInfo = downloads[i]
        file = File(downloadInfo.file)
        fileLength = file.length()
        fileExist = file.exists()
        when (downloadInfo.status) {
            Status.PAUSED,
            Status.COMPLETED,
            Status.CANCELLED,
            Status.REMOVED,
            Status.FAILED,
            Status.QUEUED -> {
                if (!fileExist && downloadInfo.status != Status.QUEUED) {
                    downloadInfo.status = Status.FAILED
                    downloadInfo.error = Error.FILE_NOT_FOUND
                    downloadInfo.downloaded = 0L
                    downloadInfo.total = -1L
                    changedDownloadsList.add(downloadInfo)
                } else {
                    update = false
                    if (downloadInfo.downloaded != fileLength) {
                        downloadInfo.downloaded = fileLength
                        update = true
                    }
                    if (downloadInfo.status == Status.COMPLETED && downloadInfo.total < 1
                            && downloadInfo.downloaded > 0) {
                        downloadInfo.total = downloadInfo.downloaded
                        update = true
                    }
                    if (update) {
                        changedDownloadsList.add(downloadInfo)
                    }
                }
            }
            Status.DOWNLOADING -> {
                if (initializing) {
                    downloadInfo.status = Status.QUEUED
                    if (fileExist) {
                        downloadInfo.downloaded = fileLength
                    }
                    changedDownloadsList.add(downloadInfo)
                }
            }
            Status.NONE,
            Status.DELETED -> {
            }
        }
    }
    if (changedDownloadsList.size > 0) {
        try {
            updateNoLock(changedDownloadsList)
        } catch (e: Exception) {
            logger.e("Database sanitize update error", e)
        }
    }
    return changedDownloadsList.size > 0
}

@JvmOverloads
fun DatabaseManager.sanitize(downloadInfo: DownloadInfo?, initializing: Boolean = false): Boolean {
    return if (downloadInfo == null) {
        false
    } else {
        sanitize(listOf(downloadInfo), initializing)
    }
}
