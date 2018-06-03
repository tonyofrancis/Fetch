package com.tonyodev.fetch2.database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.content.Context
import android.database.sqlite.SQLiteException
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.migration.Migration
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.exception.FetchImplementationException
import com.tonyodev.fetch2.util.sanitize


class DatabaseManagerImpl constructor(context: Context,
                                      private val namespace: String,
                                      override val logger: Logger,
                                      migrations: Array<Migration>) : DatabaseManager {

    private val lock = Object()

    @Volatile
    private var closed = false

    override val isClosed: Boolean
        get() = closed

    private val requestDatabase = {
        val builder = Room.databaseBuilder(context, DownloadDatabase::class.java,
                "$namespace.db")
        builder.addMigrations(*migrations)
        builder.build()
    }()

    val database: SupportSQLiteDatabase = requestDatabase.openHelper.writableDatabase

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
                val pair = Pair(downloadInfoList[it],
                        requestDatabase.wasRowInserted(rowsList[it]))
                pair
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
            logger.d("Cleared Database $namespace.db")
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
                logger.e("DatabaseManager exception", e)
            }
            try {
                database.endTransaction()
            } catch (e: SQLiteException) {
                logger.e("DatabaseManager exception", e)
            }
        }
    }

    override fun get(): List<DownloadInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val downloads = requestDatabase.requestDao().get()
            sanitize(downloads)
            return downloads
        }
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

    override fun close() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
            requestDatabase.close()
            logger.d("Database closed")
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchImplementationException("$namespace database is closed",
                    FetchException.Code.CLOSED)
        }
    }

}