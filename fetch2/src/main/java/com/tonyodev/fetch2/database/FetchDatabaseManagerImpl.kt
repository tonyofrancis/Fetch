package com.tonyodev.fetch2.database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import com.tonyodev.fetch2.PrioritySort
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.migration.Migration
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.LiveSettings
import com.tonyodev.fetch2.util.defaultNoError
import com.tonyodev.fetch2core.DefaultStorageResolver
import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.Logger


class FetchDatabaseManagerImpl constructor(context: Context,
                                           private val namespace: String,
                                           override val logger: Logger,
                                           migrations: Array<Migration>,
                                           private val liveSettings: LiveSettings,
                                           private val fileExistChecksEnabled: Boolean,
                                           private val defaultStorageResolver: DefaultStorageResolver) : FetchDatabaseManager<DownloadInfo> {

    @Volatile
    private var closed = false
    override val isClosed: Boolean
        get() {
            return closed
        }
    override var delegate: FetchDatabaseManager.Delegate<DownloadInfo>? = null
    private val requestDatabase: DownloadDatabase
    private val database: SupportSQLiteDatabase

    init {
        val builder = Room.databaseBuilder(context, DownloadDatabase::class.java, "$namespace.db")
        builder.addMigrations(*migrations)
        requestDatabase = builder.build()
        database = requestDatabase.openHelper.writableDatabase
    }

    override fun insert(downloadInfo: DownloadInfo): Pair<DownloadInfo, Boolean> {
        throwExceptionIfClosed()
        val row = requestDatabase.requestDao().insert(downloadInfo)
        return Pair(downloadInfo, requestDatabase.wasRowInserted(row))
    }

    override fun insert(downloadInfoList: List<DownloadInfo>): List<Pair<DownloadInfo, Boolean>> {
        throwExceptionIfClosed()
        val rowsList = requestDatabase.requestDao().insert(downloadInfoList)
        return rowsList.indices.map {
            Pair(downloadInfoList[it], requestDatabase.wasRowInserted(rowsList[it]))
        }
    }

    override fun delete(downloadInfo: DownloadInfo) {
        throwExceptionIfClosed()
        requestDatabase.requestDao().delete(downloadInfo)
    }

    override fun delete(downloadInfoList: List<DownloadInfo>) {
        throwExceptionIfClosed()
        requestDatabase.requestDao().delete(downloadInfoList)
    }

    override fun deleteAll() {
        throwExceptionIfClosed()
        requestDatabase.requestDao().deleteAll()
        logger.d("Cleared Database $namespace")
    }

    override fun update(downloadInfo: DownloadInfo) {
        throwExceptionIfClosed()
        requestDatabase.requestDao().update(downloadInfo)
    }

    override fun update(downloadInfoList: List<DownloadInfo>) {
        throwExceptionIfClosed()
        requestDatabase.requestDao().update(downloadInfoList)
    }

    override fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo) {
        throwExceptionIfClosed()
        try {
            database.beginTransaction()
            database.execSQL("UPDATE ${DownloadDatabase.TABLE_NAME} SET "
                    + "${DownloadDatabase.COLUMN_DOWNLOADED} = ${downloadInfo.downloaded}, "
                    + "${DownloadDatabase.COLUMN_TOTAL} = ${downloadInfo.total}, "
                    + "${DownloadDatabase.COLUMN_STATUS} = ${downloadInfo.status.value} "
                    + "WHERE ${DownloadDatabase.COLUMN_ID} = ${downloadInfo.id}")
            database.setTransactionSuccessful()
        } catch (e: SQLiteException) {
            logger.e("DatabaseManager exception", e)
        }
        try {
            database.endTransaction()
        } catch (e: SQLiteException) {
            logger.e("DatabaseManager exception", e)
        }
    }

    override fun updateExtras(id: Int, extras: Extras): DownloadInfo? {
        throwExceptionIfClosed()
        database.beginTransaction()
        database.execSQL("UPDATE ${DownloadDatabase.TABLE_NAME} SET "
                + "${DownloadDatabase.COLUMN_EXTRAS} = '${extras.toJSONString()}' "
                + "WHERE ${DownloadDatabase.COLUMN_ID} = $id")
        database.setTransactionSuccessful()
        database.endTransaction()
        val download = requestDatabase.requestDao().get(id)
        sanitize(download)
        return download
    }

    override fun get(): List<DownloadInfo> {
        throwExceptionIfClosed()
        val downloads = requestDatabase.requestDao().get()
        sanitize(downloads)
        return downloads
    }

    override fun get(id: Int): DownloadInfo? {
        throwExceptionIfClosed()
        val download = requestDatabase.requestDao().get(id)
        sanitize(download)
        return download
    }

    override fun get(ids: List<Int>): List<DownloadInfo?> {
        throwExceptionIfClosed()
        val downloads = requestDatabase.requestDao().get(ids)
        sanitize(downloads)
        return downloads
    }

    override fun getByFile(file: String): DownloadInfo? {
        throwExceptionIfClosed()
        val download = requestDatabase.requestDao().getByFile(file)
        sanitize(download)
        return download
    }

    override fun getByStatus(status: Status): List<DownloadInfo> {
        throwExceptionIfClosed()
        var downloads = requestDatabase.requestDao().getByStatus(status)
        if (sanitize(downloads)) {
            downloads = downloads.filter { it.status == status }
        }
        return downloads
    }

    override fun getByStatus(statuses: List<Status>): List<DownloadInfo> {
        throwExceptionIfClosed()
        var downloads = requestDatabase.requestDao().getByStatus(statuses)
        if (sanitize(downloads)) {
            downloads = downloads.filter { statuses.contains(it.status) }
        }
        return downloads
    }

    override fun getByGroup(group: Int): List<DownloadInfo> {
        throwExceptionIfClosed()
        val downloads = requestDatabase.requestDao().getByGroup(group)
        sanitize(downloads)
        return downloads
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, statuses: List<Status>): List<DownloadInfo> {
        throwExceptionIfClosed()
            var downloads = requestDatabase.requestDao().getByGroupWithStatus(groupId, statuses)
        if (sanitize(downloads)) {
            downloads = downloads.filter { download ->
                statuses.any { it == download.status }
            }
        }
        return downloads
    }

    override fun getDownloadsByRequestIdentifier(identifier: Long): List<DownloadInfo> {
        throwExceptionIfClosed()
        val downloads = requestDatabase.requestDao().getDownloadsByRequestIdentifier(identifier)
        sanitize(downloads)
        return downloads
    }

    override fun getPendingDownloadsSorted(prioritySort: PrioritySort): List<DownloadInfo> {
        throwExceptionIfClosed()
        var downloads = if (prioritySort == PrioritySort.ASC) {
            requestDatabase.requestDao().getPendingDownloadsSorted(Status.QUEUED)
        } else {
            requestDatabase.requestDao().getPendingDownloadsSortedDesc(Status.QUEUED)
        }
        if (sanitize(downloads)) {
            downloads = downloads.filter { it.status == Status.QUEUED }
        }
        return downloads
    }

    override fun getAllGroupIds(): List<Int> {
        throwExceptionIfClosed()
        return requestDatabase.requestDao().getAllGroupIds()
    }

    override fun getDownloadsByTag(tag: String): List<DownloadInfo> {
        throwExceptionIfClosed()
        val downloads = requestDatabase.requestDao().getDownloadsByTag(tag)
        sanitize(downloads)
        return downloads
    }

    private val pendingCountQuery = "SELECT ${DownloadDatabase.COLUMN_ID} FROM ${DownloadDatabase.TABLE_NAME}" +
            " WHERE ${DownloadDatabase.COLUMN_STATUS} = '${Status.QUEUED.value}'" +
            " OR ${DownloadDatabase.COLUMN_STATUS} = '${Status.DOWNLOADING.value}'"

    private val pendingCountIncludeAddedQuery = "SELECT ${DownloadDatabase.COLUMN_ID} FROM ${DownloadDatabase.TABLE_NAME}" +
            " WHERE ${DownloadDatabase.COLUMN_STATUS} = '${Status.QUEUED.value}'" +
            " OR ${DownloadDatabase.COLUMN_STATUS} = '${Status.DOWNLOADING.value}'" +
            " OR ${DownloadDatabase.COLUMN_STATUS} = '${Status.ADDED.value}'"

    override fun getPendingCount(includeAddedDownloads: Boolean): Long {
        return try {
            val query = if (includeAddedDownloads) pendingCountIncludeAddedQuery else pendingCountQuery
            val cursor: Cursor? = database.query(query)
            val count = cursor?.count?.toLong() ?: -1L
            cursor?.close()
            count
        } catch (e: Exception) {
            -1
        }
    }

    override fun sanitizeOnFirstEntry() {
        throwExceptionIfClosed()
        liveSettings.execute {
            if (!it.didSanitizeDatabaseOnFirstEntry) {
                sanitize(get(), true)
                it.didSanitizeDatabaseOnFirstEntry = true
            }
        }
    }

    private val updatedDownloadsList = mutableListOf<DownloadInfo>()

    private fun sanitize(downloads: List<DownloadInfo>, firstEntry: Boolean = false): Boolean {
        updatedDownloadsList.clear()
        var downloadInfo: DownloadInfo
        for (i in 0 until downloads.size) {
            downloadInfo = downloads[i]
            when (downloadInfo.status) {
                Status.COMPLETED -> onCompleted(downloadInfo)
                Status.DOWNLOADING -> onDownloading(downloadInfo, firstEntry)
                Status.QUEUED,
                Status.PAUSED -> onPaused(downloadInfo)
                Status.CANCELLED,
                Status.FAILED,
                Status.ADDED,
                Status.NONE,
                Status.DELETED,
                Status.REMOVED -> {
                }
            }
        }
        val updatedCount = updatedDownloadsList.size
        if (updatedCount > 0) {
            try {
                update(updatedDownloadsList)
            } catch (e: Exception) {
                logger.e("Failed to update", e)
            }
        }
        updatedDownloadsList.clear()
        return updatedCount > 0
    }

    private fun onPaused(downloadInfo: DownloadInfo) {
        if (downloadInfo.downloaded > 0) {
            if (fileExistChecksEnabled) {
                if (!defaultStorageResolver.fileExists(downloadInfo.file)) {
                    downloadInfo.downloaded = 0
                    downloadInfo.total = -1L
                    downloadInfo.error = defaultNoError
                    updatedDownloadsList.add(downloadInfo)
                    delegate?.deleteTempFilesForDownload(downloadInfo)
                }
            }
        }
    }

    private fun onDownloading(downloadInfo: DownloadInfo, firstEntry: Boolean) {
        if (firstEntry) {
            val status = if (downloadInfo.downloaded > 0 && downloadInfo.total > 0 && downloadInfo.downloaded >= downloadInfo.total) {
                Status.COMPLETED
            } else {
                Status.QUEUED
            }
            downloadInfo.status = status
            downloadInfo.error = defaultNoError
            updatedDownloadsList.add(downloadInfo)
        }
    }

    private fun onCompleted(downloadInfo: DownloadInfo) {
        if (downloadInfo.total < 1 && downloadInfo.downloaded > 0) {
            downloadInfo.total = downloadInfo.downloaded
            downloadInfo.error = defaultNoError
            updatedDownloadsList.add(downloadInfo)
        }
    }

    private fun sanitize(downloadInfo: DownloadInfo?, initializing: Boolean = false): Boolean {
        return if (downloadInfo == null) {
            false
        } else {
            sanitize(listOf(downloadInfo), initializing)
        }
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        requestDatabase.close()
        logger.d("Database closed")
    }

    override fun getNewDownloadInfoInstance(): DownloadInfo {
        return DownloadInfo()
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchException("$namespace database is closed")
        }
    }

}