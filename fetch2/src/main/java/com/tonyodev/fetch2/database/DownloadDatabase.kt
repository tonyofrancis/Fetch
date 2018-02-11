package com.tonyodev.fetch2.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import com.tonyodev.fetch2.database.DownloadDatabase.Companion.DATABASE_CURRENT_VERSION
import com.tonyodev.fetch2.database.DownloadDatabase.Companion.EXPORTED_SCHEMA
import com.tonyodev.fetch2.database.migration.DownloadMigrationOneToTwo
import com.tonyodev.fetch2.database.migration.Migration

@Database(entities = [DownloadInfo::class], version = DATABASE_CURRENT_VERSION, exportSchema = EXPORTED_SCHEMA)
@TypeConverters(value = [Converter::class])
abstract class DownloadDatabase : RoomDatabase() {

    abstract fun requestDao(): DownloadDao

    fun wasRowInserted(row: Long): Boolean {
        return row != (-1).toLong()
    }

    companion object {
        const val TABLE_NAME = "requests"
        const val COLUMN_ID = "_id"
        const val COLUMN_NAMESPACE = "_namespace"
        const val COLUMN_URL = "_url"
        const val COLUMN_FILE = "_file"
        const val COLUMN_GROUP = "_group"
        const val COLUMN_PRIORITY = "_priority"
        const val COLUMN_HEADERS = "_headers"
        const val COLUMN_DOWNLOADED = "_written_bytes"
        const val COLUMN_TOTAL = "_total_bytes"
        const val COLUMN_STATUS = "_status"
        const val COLUMN_ERROR = "_error"
        const val COLUMN_NETWORK_TYPE = "_network_type"
        const val COLUMN_CREATED = "_created"
        const val COLUMN_TAG = "_tag"
        const val DATABASE_CURRENT_VERSION = 2
        const val DATABASE_OLD_VERSION = 1
        const val EXPORTED_SCHEMA = false

        @JvmStatic
        fun getMigrations(): Array<Migration> {
            return arrayOf(DownloadMigrationOneToTwo())
        }

    }

}