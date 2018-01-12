package com.tonyodev.fetch2.provider

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.DatabaseManager


open class DownloadProvider(val databaseManagerInternal: DatabaseManager) {

    open fun getDownloads(): List<Download> {
        return databaseManagerInternal.get()
    }

    open fun getDownload(id: Int): Download? {
        return databaseManagerInternal.get(id)
    }

    open fun getDownloads(ids: List<Int>): List<Download?> {
        return databaseManagerInternal.get(ids)
    }

    open fun getByGroup(group: Int): List<Download> {
        return databaseManagerInternal.getByGroup(group)
    }

    open fun getByStatus(status: Status): List<Download> {
        return databaseManagerInternal.getByStatus(status)
    }

}