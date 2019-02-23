package com.tonyodev.fetch2.provider

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.FetchDatabaseManager


class DownloadProvider(private val fetchDatabaseManager: FetchDatabaseManager) {

    fun getDownloads(): List<Download> {
        return fetchDatabaseManager.get()
    }

    fun getDownload(id: Int): Download? {
        return fetchDatabaseManager.get(id)
    }

    fun getDownloads(ids: List<Int>): List<Download?> {
        return fetchDatabaseManager.get(ids)
    }

    fun getByGroup(group: Int): List<Download> {
        return fetchDatabaseManager.getByGroup(group)
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
        return fetchDatabaseManager.getByStatus(status)
    }

    fun getPendingDownloadsSorted(): List<Download> {
        return fetchDatabaseManager.getPendingDownloadsSorted()
    }

}