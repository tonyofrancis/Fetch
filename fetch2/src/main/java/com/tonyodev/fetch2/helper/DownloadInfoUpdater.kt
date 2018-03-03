package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.database.DatabaseManager
import com.tonyodev.fetch2.database.DownloadInfo


class DownloadInfoUpdater(private val databaseManager: DatabaseManager) {

    fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo) {
        databaseManager.updateFileBytesInfoAndStatusOnly(downloadInfo)
    }

    fun update(downloadInfo: DownloadInfo) {
        databaseManager.update(downloadInfo)
    }
}