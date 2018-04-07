@file:JvmName("FetchDatabaseExtensions")

package com.tonyodev.fetch2.util

import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.DatabaseManager
import java.io.File

fun DatabaseManager.verifyDatabase() {
    val downloads = get()
    val downloadingStatus = Status.DOWNLOADING
    downloads.forEach {
        if (it.status == downloadingStatus) {
            it.status = Status.QUEUED
        }
        val file = File(it.file)
        if (file.exists()) {
            it.downloaded = file.length()
        } else {
            when (it.status) {
                Status.PAUSED,
                Status.COMPLETED,
                Status.CANCELLED,
                Status.REMOVED -> {
                    it.status = Status.FAILED
                    it.error = Error.FILE_NOT_FOUND
                    it.downloaded = 0L
                    it.total = -1L
                }
                else -> {
                }
            }
        }
    }
    if (downloads.isNotEmpty()) {
        try {
            update(downloads)
        } catch (e: Exception) {
            logger.e("Database verification update error", e)
        }
    }
}
