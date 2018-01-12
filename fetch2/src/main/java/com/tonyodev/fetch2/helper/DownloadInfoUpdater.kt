package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.database.DatabaseManager
import com.tonyodev.fetch2.database.DownloadInfo


open class DownloadInfoUpdater(val databaseManagerInternal: DatabaseManager) {

    open fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo) {
        databaseManagerInternal.updateFileBytesInfoAndStatusOnly(downloadInfo)
    }

    open fun update(downloadInfo: DownloadInfo) {
        databaseManagerInternal.update(downloadInfo)
    }
}