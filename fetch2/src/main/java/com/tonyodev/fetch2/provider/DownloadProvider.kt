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

    fun getByGroupReplace(group: Int, download: Download): List<Download> {
        val downloads = getByGroup(group) as ArrayList
        val index = downloads.indexOfFirst { it.id == download.id }
        if (index != -1) {
            downloads[index] = download
        }
        return downloads
    }

    fun getByStatus(status: Status): List<Download> {
        return databaseManager.getByStatus(status)
    }

    fun getPendingDownloadsSorted(): List<Download> {
        return databaseManager.getPendingDownloadsSorted()
    }

}