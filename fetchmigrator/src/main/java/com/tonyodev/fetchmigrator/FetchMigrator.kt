@file:JvmName("FetchMigrator")

package com.tonyodev.fetchmigrator

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.support.annotation.WorkerThread
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.database.DatabaseManagerImpl
import com.tonyodev.fetch2.database.DownloadDatabase
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetchmigrator.fetch1.DatabaseHelper
import com.tonyodev.fetchmigrator.helpers.v1CursorToV2DownloadInfo
import java.io.File
import java.io.IOException
import java.sql.SQLException

@WorkerThread
@Throws(exceptionClasses = [SQLException::class, SQLiteConstraintException::class])
fun migrateFromV1toV2(context: Context, v2Namespace: String): List<Download> {
    val fetchOneDatabaseHelper = DatabaseHelper(context)
    fetchOneDatabaseHelper.clean()
    fetchOneDatabaseHelper.verifyOK()
    val downloadInfoList = mutableListOf<DownloadInfo>()
    val cursor = fetchOneDatabaseHelper.get()
    if (cursor != null) {
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val downloadInfo = v1CursorToV2DownloadInfo(cursor)
            try {
                val file = File(downloadInfo.file)
                if (file.exists()) {
                    downloadInfo.downloaded = file.length()
                }
            } catch (e: IOException) {

            }
            downloadInfo.namespace = v2Namespace
            downloadInfoList.add(downloadInfo)
            cursor.moveToNext()
        }
        cursor.close()
        val fetchTwoDatabaseManager = DatabaseManagerImpl(
                context = context,
                namespace = v2Namespace,
                isMemoryDatabase = false,
                logger = FetchLogger(),
                migrations = DownloadDatabase.getMigrations())

        fetchTwoDatabaseManager.insert(downloadInfoList)
        fetchTwoDatabaseManager.close()
    }
    fetchOneDatabaseHelper.close()
    return downloadInfoList
}

fun deleteFetchV1Database(context: Context) {
    context.deleteDatabase(DatabaseHelper.DB_NAME)
}

