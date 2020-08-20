package com.tonyodev.fetch2.database

import com.tonyodev.fetch2.PrioritySort
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.Logger

class FetchDatabaseManagerWrapper(private val fetchDatabaseManager: FetchDatabaseManager<DownloadInfo>): FetchDatabaseManager<DownloadInfo> {

    override val logger: Logger = fetchDatabaseManager.logger
    private val lock = Any()

    override val isClosed: Boolean
        get() {
            return synchronized(lock) {
                fetchDatabaseManager.isClosed
            }
        }

    override var delegate: FetchDatabaseManager.Delegate<DownloadInfo>?
        get() {
            return synchronized(lock) {
                fetchDatabaseManager.delegate
            }
        }
        set(value) {
            synchronized(lock) {
                fetchDatabaseManager.delegate = value
            }
        }

    override fun insert(downloadInfo: DownloadInfo): Pair<DownloadInfo, Boolean> {
        return synchronized(lock) {
            fetchDatabaseManager.insert(downloadInfo)
        }
    }

    override fun insert(downloadInfoList: List<DownloadInfo>): List<Pair<DownloadInfo, Boolean>> {
        return synchronized(lock) {
            fetchDatabaseManager.insert(downloadInfoList)
        }
    }

    override fun delete(downloadInfo: DownloadInfo) {
        synchronized(lock) {
            fetchDatabaseManager.delete(downloadInfo)
        }
    }

    override fun delete(downloadInfoList: List<DownloadInfo>) {
        synchronized(lock) {
            fetchDatabaseManager.delete(downloadInfoList)
        }
    }

    override fun deleteAll() {
        synchronized(lock) {
            fetchDatabaseManager.deleteAll()
        }
    }

    override fun update(downloadInfo: DownloadInfo) {
        synchronized(lock) {
            fetchDatabaseManager.update(downloadInfo)
        }
    }

    override fun update(downloadInfoList: List<DownloadInfo>) {
        return synchronized(lock) {
            fetchDatabaseManager.update(downloadInfoList)
        }
    }

    override fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo) {
        return synchronized(lock) {
            fetchDatabaseManager.updateFileBytesInfoAndStatusOnly(downloadInfo)
        }
    }

    override fun get(): List<DownloadInfo> {
        return synchronized(lock) {
            fetchDatabaseManager.get()
        }
    }

    override fun get(id: Int): DownloadInfo? {
        return synchronized(lock) {
            fetchDatabaseManager.get(id)
        }
    }

    override fun get(ids: List<Int>): List<DownloadInfo?> {
        return synchronized(lock) {
            fetchDatabaseManager.get(ids)
        }
    }

    override fun getByFile(file: String): DownloadInfo? {
        return synchronized(lock) {
            fetchDatabaseManager.getByFile(file)
        }
    }

    override fun getByStatus(status: Status): List<DownloadInfo> {
        return synchronized(lock) {
            fetchDatabaseManager.getByStatus(status)
        }
    }

    override fun getByStatus(statuses: List<Status>): List<DownloadInfo> {
        return synchronized(lock) {
            fetchDatabaseManager.getByStatus(statuses)
        }
    }

    override fun getByGroup(group: Int): List<DownloadInfo> {
        return synchronized(lock) {
            fetchDatabaseManager.getByGroup(group)
        }
    }

    override fun getAllGroupIds(): List<Int> {
        return synchronized(lock) {
            fetchDatabaseManager.getAllGroupIds()
        }
    }

    override fun getDownloadsByTag(tag: String): List<DownloadInfo> {
        return synchronized(lock) {
            fetchDatabaseManager.getDownloadsByTag(tag)
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, statuses: List<Status>): List<DownloadInfo> {
        return synchronized(lock) {
            fetchDatabaseManager.getDownloadsInGroupWithStatus(groupId, statuses)
        }
    }

    override fun getDownloadsByRequestIdentifier(identifier: Long): List<DownloadInfo> {
        return synchronized(lock) {
            fetchDatabaseManager.getDownloadsByRequestIdentifier(identifier)
        }
    }

    override fun getPendingDownloadsSorted(prioritySort: PrioritySort): List<DownloadInfo> {
        return synchronized(lock) {
            fetchDatabaseManager.getPendingDownloadsSorted(prioritySort)
        }
    }

    override fun sanitizeOnFirstEntry() {
       synchronized(lock) {
           fetchDatabaseManager.sanitizeOnFirstEntry()
       }
    }

    override fun updateExtras(id: Int, extras: Extras): DownloadInfo? {
       return synchronized(lock) {
           fetchDatabaseManager.updateExtras(id, extras)
       }
    }

    override fun getPendingCount(includeAddedDownloads: Boolean): Long {
        return synchronized(lock) {
            fetchDatabaseManager.getPendingCount(includeAddedDownloads)
        }
    }

    override fun getNewDownloadInfoInstance(): DownloadInfo {
        return fetchDatabaseManager.getNewDownloadInfoInstance()
    }

    override fun close() {
        synchronized(lock) {
            fetchDatabaseManager.close()
        }
    }

}