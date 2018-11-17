package com.tonyodev.fetch2fileserver.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.tonyodev.fetch2fileserver.database.FileResourceInfoDatabase.Companion.DATABASE_VERSION

@Database(entities = [FileResourceInfo::class], version = DATABASE_VERSION, exportSchema = false)
abstract class FileResourceInfoDatabase : RoomDatabase() {

    abstract fun fileResourceInfoDao(): FileResourceInfoDao

    companion object {
        const val TABLE_NAME = "fileResourceInfo"
        const val COLUMN_ID = "_id"
        const val COLUMN_LENGTH = "_length"
        const val COLUMN_FILE = "_file"
        const val COLUMN_NAME = "_name"
        const val COLUMN_EXTRAS = "_customData"
        const val COLUMN_MD5 = "_md5"
        const val OLD_DATABASE_VERSION = 0
        const val DATABASE_VERSION = 1
        const val MAX_PAGE_SIZE = 100

    }

}