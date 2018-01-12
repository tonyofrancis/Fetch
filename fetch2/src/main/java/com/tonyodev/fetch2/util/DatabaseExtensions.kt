@file:JvmName("FetchDatabaseExtensions")

package com.tonyodev.fetch2.util

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
