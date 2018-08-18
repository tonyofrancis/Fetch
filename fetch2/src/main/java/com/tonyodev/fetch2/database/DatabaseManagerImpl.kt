package com.tonyodev.fetch2.database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.content.Context
import android.database.sqlite.SQLiteException
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.migration.Migration
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.LiveSettings
import java.io.File


class DatabaseManagerImpl constructor(context: Context,
                                      private val namespace: String,
                                      migrations: Array<Migration>,
                                      private val liveSettings: LiveSettings) : DatabaseManager {

    private val lock = Object()
    @Volatile
    private var closed = false
    override val isClosed: Boolean
        get() {
            return closed
        }
    override val didSanitizeOnFirstEntry: Boolean
        get() {
            return liveSettings.didSanitizeDatabaseOnFirstEntry
        }
    override var delegate: DatabaseManager.Delegate? = null
    private val requestDatabase: DownloadDatabase
    private val database: SupportSQLiteDatabase

    init {
        val builder = Room.databaseBuilder(context, DownloadDatabase::class.java, "$namespace.db")
        builder.addMigrations(*migrations)
        requestDatabase = builder.build()
        database = requestDatabase.openHelper.writableDatabase
    }

    override fun insert(downloadInfo: DownloadInfo): Pair<DownloadInfo, Boolean> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val row = requestDatabase.requestDao().insert(downloadInfo)
            return Pair(downloadInfo, requestDatabase.wasRowInserted(row))
        }
    }

    override fun insert(downloadInfoList: List<DownloadInfo>): List<Pair<DownloadInfo, Boolean>> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val rowsList = requestDatabase.requestDao().insert(downloadInfoList)
            return rowsList.indices.map {
                Pair(downloadInfoList[it], requestDatabase.wasRowInserted(rowsList[it]))
            }
        }
    }

    override fun delete(downloadInfo: DownloadInfo) {
        synchronized(lock) {
            throwExceptionIfClosed()
            requestDatabase.requestDao().delete(downloadInfo)
        }
    }

    override fun delete(downloadInfoList: List<DownloadInfo>) {
        synchronized(lock) {
            throwExceptionIfClosed()
            requestDatabase.requestDao().delete(downloadInfoList)
        }
    }

    override fun deleteAll() {
        synchronized(lock) {
            throwExceptionIfClosed()
            requestDatabase.requestDao().deleteAll()
        }
    }

    override fun update(downloadInfo: DownloadInfo) {
        synchronized(lock) {
            throwExceptionIfClosed()
            requestDatabase.requestDao().update(downloadInfo)
        }
    }

    override fun update(downloadInfoList: List<DownloadInfo>) {
        synchronized(lock) {
            updateNoLock(downloadInfoList)
        }
    }

    override fun updateNoLock(downloadInfoList: List<DownloadInfo>) {
        throwExceptionIfClosed()
        requestDatabase.requestDao().update(downloadInfoList)
    }

    override fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo) {
        synchronized(lock) {
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

            }
            try {
                database.endTransaction()
            } catch (e: SQLiteException) {

            }
        }
    }

    override fun get(): List<DownloadInfo> {
        synchronized(lock) {
            return getDownloadsNoLock()
        }
    }

    private fun getDownloadsNoLock(): List<DownloadInfo> {
        throwExceptionIfClosed()
        val downloads = requestDatabase.requestDao().get()
        sanitize(downloads)
        return downloads
    }

    override fun get(id: Int): DownloadInfo? {
        synchronized(lock) {
            throwExceptionIfClosed()
            val download = requestDatabase.requestDao().get(id)
            sanitize(download)
            return download
        }
    }

    override fun get(ids: List<Int>): List<DownloadInfo?> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val downloads = requestDatabase.requestDao().get(ids)
            sanitize(downloads)
            return downloads
        }
    }

    override fun getByFile(file: String): DownloadInfo? {
        synchronized(lock) {
            throwExceptionIfClosed()
            val download = requestDatabase.requestDao().getByFile(file)
            sanitize(download)
            return download
        }
    }

    override fun getByStatus(status: Status): List<DownloadInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            var downloads = requestDatabase.requestDao().getByStatus(status)
            if (sanitize(downloads)) {
                downloads = downloads.filter { it.status == status }
            }
            return downloads
        }
    }

    override fun getByGroup(group: Int): List<DownloadInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val downloads = requestDatabase.requestDao().getByGroup(group)
            sanitize(downloads)
            return downloads
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): List<DownloadInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            var downloads = requestDatabase.requestDao().getByGroupWithStatus(groupId, status)
            if (sanitize(downloads)) {
                downloads = downloads.filter { it.status == status }
            }
            return downloads
        }
    }

    override fun getDownloadsByRequestIdentifier(identifier: Long): List<DownloadInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val downloads = requestDatabase.requestDao().getDownloadsByRequestIdentifier(identifier)
            sanitize(downloads)
            return downloads
        }
    }

    override fun getPendingDownloadsSorted(): List<DownloadInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            var downloads = requestDatabase.requestDao().getPendingDownloadsSorted(Status.QUEUED)
            if (sanitize(downloads)) {
                downloads = downloads.filter { it.status == Status.QUEUED }
            }
            return downloads
        }
    }

    override fun sanitizeOnFirstEntry() {
        synchronized(lock) {
            throwExceptionIfClosed()
            liveSettings.execute {
                if (!it.didSanitizeDatabaseOnFirstEntry) {
                    sanitize(getDownloadsNoLock(), true)
                    it.didSanitizeDatabaseOnFirstEntry = true
                }
            }
        }
    }

    private fun sanitize(downloads: List<DownloadInfo>, firstEntry: Boolean = false): Boolean {
        val changedDownloadsList = mutableListOf<DownloadInfo>()
        var file: File?
        var fileExist: Boolean
        var downloadInfo: DownloadInfo
        var update: Boolean
        for (i in 0 until downloads.size) {
            downloadInfo = downloads[i]
            file = File(downloadInfo.file)
            fileExist = file.exists()
            when (downloadInfo.status) {
                Status.PAUSED,
                Status.COMPLETED,
                Status.CANCELLED,
                Status.FAILED,
                Status.QUEUED -> {
                    if (!fileExist) {
                        downloadInfo.status = Status.FAILED
                        downloadInfo.error = Error.FILE_NOT_FOUND
                        downloadInfo.downloaded = 0L
                        downloadInfo.total = -1L
                        changedDownloadsList.add(downloadInfo)
                        delegate?.deleteTempFilesForDownload(downloadInfo)
                    } else {
                        update = false
                        if (downloadInfo.status == Status.COMPLETED && downloadInfo.total < 1
                                && downloadInfo.downloaded > 0 && fileExist) {
                            downloadInfo.total = downloadInfo.downloaded
                            update = true
                        }
                        if (update) {
                            changedDownloadsList.add(downloadInfo)
                        }
                    }
                }
                Status.DOWNLOADING -> {
                    if (firstEntry) {
                        downloadInfo.status = Status.QUEUED
                        changedDownloadsList.add(downloadInfo)
                    }
                }
                Status.ADDED,
                Status.NONE,
                Status.DELETED,
                Status.REMOVED -> {
                }
            }
        }
        if (changedDownloadsList.size > 0) {
            try {
                updateNoLock(changedDownloadsList)
            } catch (e: Exception) {
            }
        }
        return changedDownloadsList.size > 0
    }

    private fun sanitize(downloadInfo: DownloadInfo?, initializing: Boolean = false): Boolean {
        return if (downloadInfo == null) {
            false
        } else {
            sanitize(listOf(downloadInfo), initializing)
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
            requestDatabase.close()
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchException("$namespace database is closed")
        }
    }

}