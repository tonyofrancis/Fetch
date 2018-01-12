package com.tonyodev.fetch2.database

import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.Status
import java.io.Closeable


interface DatabaseManager : Closeable {

    val isClosed: Boolean
    val isMemoryDatabase: Boolean
    val logger: Logger

    fun insert(downloadInfo: DownloadInfo): Pair<DownloadInfo, Boolean>
    fun insert(downloadInfoList: List<DownloadInfo>): List<Pair<DownloadInfo, Boolean>>
    fun delete(downloadInfo: DownloadInfo)
    fun delete(downloadInfoList: List<DownloadInfo>)
    fun deleteAll()
    fun update(downloadInfo: DownloadInfo)
    fun update(downloadInfoList: List<DownloadInfo>)
    fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo)
    fun get(): List<DownloadInfo>
    fun get(id: Int): DownloadInfo?
    fun get(ids: List<Int>): List<DownloadInfo?>
    fun getByStatus(status: Status): List<DownloadInfo>
    fun getByGroup(group: Int): List<DownloadInfo>
    fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): List<DownloadInfo>
}