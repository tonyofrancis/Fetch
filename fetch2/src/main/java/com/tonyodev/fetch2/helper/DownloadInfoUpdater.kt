package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.database.FetchDatabaseManager
import com.tonyodev.fetch2.database.DownloadInfo


class DownloadInfoUpdater(private val fetchDatabaseManager: FetchDatabaseManager) {

    fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo) {
        fetchDatabaseManager.updateFileBytesInfoAndStatusOnly(downloadInfo)
    }

    fun update(downloadInfo: DownloadInfo) {
        fetchDatabaseManager.update(downloadInfo)
    }

}