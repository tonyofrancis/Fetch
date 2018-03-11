package com.tonyodev.fetch2.provider

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.DatabaseManager


class DownloadProvider(private val databaseManager: DatabaseManager) {

    fun getDownloads(): List<Download> {
        return databaseManager.get()
    }

    fun getDownload(id: Int): Download? {
        return databaseManager.get(id)
    }

    fun getDownloads(ids: List<Int>): List<Download?> {
        return databaseManager.get(ids)
    }

    fun getByGroup(group: Int): List<Download> {
        return databaseManager.getByGroup(group)
    }

    fun getByStatus(status: Status): List<Download> {
        return databaseManager.getByStatus(status)
    }

    fun getPendingDownloadsSorted(): List<Download> {
        return databaseManager.getPendingDownloadsSorted()
    }

}