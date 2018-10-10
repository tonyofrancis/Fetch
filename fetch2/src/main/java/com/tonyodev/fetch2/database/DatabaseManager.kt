package com.tonyodev.fetch2.database

import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.Extras
import java.io.Closeable


interface DatabaseManager : Closeable {

    val isClosed: Boolean
    val didSanitizeOnFirstEntry: Boolean
    var delegate: DatabaseManager.Delegate?

    fun insert(downloadInfo: DownloadInfo): Pair<DownloadInfo, Boolean>
    fun insert(downloadInfoList: List<DownloadInfo>): List<Pair<DownloadInfo, Boolean>>
    fun delete(downloadInfo: DownloadInfo)
    fun delete(downloadInfoList: List<DownloadInfo>)
    fun deleteAll()
    fun update(downloadInfo: DownloadInfo)
    fun update(downloadInfoList: List<DownloadInfo>)
    fun updateNoLock(downloadInfoList: List<DownloadInfo>)
    fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo)
    fun get(): List<DownloadInfo>
    fun get(id: Int): DownloadInfo?
    fun get(ids: List<Int>): List<DownloadInfo?>
    fun getByFile(file: String): DownloadInfo?
    fun getByStatus(status: Status): List<DownloadInfo>
    fun getByGroup(group: Int): List<DownloadInfo>
    fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): List<DownloadInfo>
    fun getDownloadsByRequestIdentifier(identifier: Long): List<DownloadInfo>
    fun getPendingDownloadsSorted(): List<DownloadInfo>
    fun sanitizeOnFirstEntry()
    fun updateExtras(id: Int, extras: Extras): DownloadInfo?

    interface Delegate {
        fun deleteTempFilesForDownload(downloadInfo: DownloadInfo)
    }
}