package com.tonyodev.fetch2.database

import com.tonyodev.fetch2.PrioritySort
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.Logger

class FetchDatabaseManagerWrapper(private val fetchDatabaseManager: FetchDatabaseManager<DownloadInfo>): FetchDatabaseManager<DownloadInfo> {

    override val logger: Logger = fetchDatabaseManager.logger

    override val isClosed: Boolean
        get() {
            return synchronized(fetchDatabaseManager) {
                fetchDatabaseManager.isClosed
            }
        }

    override var delegate: FetchDatabaseManager.Delegate<DownloadInfo>?
        get() {
            return synchronized(fetchDatabaseManager) {
                fetchDatabaseManager.delegate
            }
        }
        set(value) {
            synchronized(fetchDatabaseManager) {
                fetchDatabaseManager.delegate = value
            }
        }

    override fun insert(downloadInfo: DownloadInfo): Pair<DownloadInfo, Boolean> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.insert(downloadInfo)
        }
    }

    override fun insert(downloadInfoList: List<DownloadInfo>): List<Pair<DownloadInfo, Boolean>> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.insert(downloadInfoList)
        }
    }

    override fun delete(downloadInfo: DownloadInfo) {
        synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.delete(downloadInfo)
        }
    }

    override fun delete(downloadInfoList: List<DownloadInfo>) {
        synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.delete(downloadInfoList)
        }
    }

    override fun deleteAll() {
        synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.deleteAll()
        }
    }

    override fun update(downloadInfo: DownloadInfo) {
        synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.update(downloadInfo)
        }
    }

    override fun update(downloadInfoList: List<DownloadInfo>) {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.update(downloadInfoList)
        }
    }

    override fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo) {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.updateFileBytesInfoAndStatusOnly(downloadInfo)
        }
    }

    override fun get(): List<DownloadInfo> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.get()
        }
    }

    override fun get(id: Int): DownloadInfo? {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.get(id)
        }
    }

    override fun get(ids: List<Int>): List<DownloadInfo?> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.get(ids)
        }
    }

    override fun getByFile(file: String): DownloadInfo? {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.getByFile(file)
        }
    }

    override fun getByStatus(status: Status): List<DownloadInfo> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.getByStatus(status)
        }
    }

    override fun getByStatus(statuses: List<Status>): List<DownloadInfo> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.getByStatus(statuses)
        }
    }

    override fun getByGroup(group: Int): List<DownloadInfo> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.getByGroup(group)
        }
    }

    override fun getAllGroupIds(): List<Int> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.getAllGroupIds()
        }
    }

    override fun getDownloadsByTag(tag: String): List<DownloadInfo> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.getDownloadsByTag(tag)
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, statuses: List<Status>): List<DownloadInfo> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.getDownloadsInGroupWithStatus(groupId, statuses)
        }
    }

    override fun getDownloadsByRequestIdentifier(identifier: Long): List<DownloadInfo> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.getDownloadsByRequestIdentifier(identifier)
        }
    }

    override fun getPendingDownloadsSorted(prioritySort: PrioritySort): List<DownloadInfo> {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.getPendingDownloadsSorted(prioritySort)
        }
    }

    override fun sanitizeOnFirstEntry() {
       synchronized(fetchDatabaseManager) {
           fetchDatabaseManager.sanitizeOnFirstEntry()
       }
    }

    override fun updateExtras(id: Int, extras: Extras): DownloadInfo? {
       return synchronized(fetchDatabaseManager) {
           fetchDatabaseManager.updateExtras(id, extras)
       }
    }

    override fun getPendingCount(includeAddedDownloads: Boolean): Long {
        return synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.getPendingCount(includeAddedDownloads)
        }
    }

    override fun getNewDownloadInfoInstance(): DownloadInfo {
        return fetchDatabaseManager.getNewDownloadInfoInstance()
    }

    override fun close() {
        synchronized(fetchDatabaseManager) {
            fetchDatabaseManager.close()
        }
    }

}