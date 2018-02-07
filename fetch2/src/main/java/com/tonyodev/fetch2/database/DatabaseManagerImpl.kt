package com.tonyodev.fetch2.database

import android.arch.persistence.room.Room
import android.content.Context
import android.database.sqlite.SQLiteException
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.exception.FetchImplementationException


class DatabaseManagerImpl constructor(context: Context,
                                      override val namespace: String,
                                      override val isMemoryDatabase: Boolean,
                                      override val logger: Logger) : DatabaseManager {

    private val lock = Object()

    @Volatile
    private var closed = false
    override val isClosed: Boolean
        get() = closed

    private val requestDatabase = {
        val builder = if (isMemoryDatabase) {
            logger.d("Init in memory database named $namespace")
            Room.inMemoryDatabaseBuilder(context, DownloadDatabase::class.java)
        } else {
            logger.d("Init file based database named $namespace.db")
            Room.databaseBuilder(context, DownloadDatabase::class.java,
                    "$namespace.db")
        }
        builder.build()
    }()

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
            throwExceptionIfClosed()
            requestDatabase.requestDao().update(downloadInfoList)
        }
    }

    override fun updateFileBytesInfoAndStatusOnly(downloadInfo: DownloadInfo) {
        synchronized(lock) {
            throwExceptionIfClosed()
            val database = requestDatabase.openHelper.writableDatabase
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
            return requestDatabase.requestDao().get()
        }
    }

    override fun get(id: Int): DownloadInfo? {
        synchronized(lock) {
            throwExceptionIfClosed()
            return requestDatabase.requestDao().get(id)
        }
    }

    override fun get(ids: List<Int>): List<DownloadInfo?> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return requestDatabase.requestDao().get(ids)
        }
    }

    override fun getByStatus(status: Status): List<DownloadInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return requestDatabase.requestDao().getByStatus(status)
        }
    }

    override fun getByGroup(group: Int): List<DownloadInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return requestDatabase.requestDao().getByGroup(group)
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): List<DownloadInfo> {
        synchronized(lock) {
            return requestDatabase.requestDao().getByGroupWithStatus(groupId, status)
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
            throw FetchImplementationException("database is closed",
                    FetchImplementationException.Code.CLOSED)
        }
    }

}