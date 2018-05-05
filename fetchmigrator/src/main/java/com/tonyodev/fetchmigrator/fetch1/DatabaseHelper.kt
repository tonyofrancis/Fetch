package com.tonyodev.fetchmigrator.fetch1

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

internal class DatabaseHelper constructor(context: Context)
    : SQLiteOpenHelper(context, DB_NAME, null, VERSION) {

    private val lock = Object()
    private val db: SQLiteDatabase = writableDatabase

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("CREATE TABLE " + TABLE_NAME + " ( "
                + COLUMN_ID + " INTEGER PRIMARY KEY NOT NULL, "
                + COLUMN_URL + " TEXT NOT NULL, "
                + COLUMN_FILEPATH + " TEXT NOT NULL, "
                + COLUMN_STATUS + " INTEGER NOT NULL, "
                + COLUMN_HEADERS + " TEXT NOT NULL, "
                + COLUMN_DOWNLOADED_BYTES + " INTEGER NOT NULL, "
                + COLUMN_FILE_SIZE + " INTEGER NOT NULL, "
                + COLUMN_ERROR + " INTEGER NOT NULL, "
                + COLUMN_PRIORITY + " INTEGER NOT NULL, "
                + "unique( " + COLUMN_FILEPATH + " ) )")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when (oldVersion) {
            1 -> {
                db.execSQL("CREATE UNIQUE INDEX table_unique ON "
                        + TABLE_NAME + " ( " + COLUMN_FILEPATH + ")")
            }
            else -> {
            }
        }
    }

    fun get(): Cursor? {
        synchronized(lock) {
            return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        }
    }

    fun verifyOK() {
        synchronized(lock) {
            db.beginTransaction()
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_STATUS + " = "
                    + FetchConst.STATUS_QUEUED + " WHERE " + COLUMN_STATUS
                    + " = " + FetchConst.STATUS_DOWNLOADING)
            db.setTransactionSuccessful()
            db.endTransaction()
        }
    }

    fun clean() {
        synchronized(lock) {
            val cursor = db.rawQuery("SELECT " + COLUMN_ID + ", " + COLUMN_FILEPATH
                    + " FROM " + TABLE_NAME + " WHERE " + COLUMN_STATUS + " = "
                    + FetchConst.STATUS_DONE, null)

            if (cursor.count < 1) {
                cursor.close()
                return
            }
            db.beginTransaction()
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val destinationUri = cursor.getString(cursor.getColumnIndex(COLUMN_FILEPATH))
                if (destinationUri != null) {
                    if (!File(destinationUri).exists()) {
                        val id = cursor.getLong(cursor.getColumnIndex(COLUMN_ID))
                        db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_STATUS + " = "
                                + FetchConst.STATUS_ERROR + ", " + COLUMN_ERROR + " = "
                                + FetchConst.ERROR_FILE_NOT_FOUND + " WHERE "
                                + COLUMN_ID + " = " + id)
                    }
                }
                cursor.moveToNext()
            }
            db.setTransactionSuccessful()
            cursor?.close()
            db.endTransaction()
        }
    }

    companion object {
        const val VERSION = 2
        const val DB_NAME = "com_tonyodev_fetch.db"
        const val TABLE_NAME = "requests"
        const val COLUMN_ID = "_id"
        const val COLUMN_URL = "_url"
        const val COLUMN_FILEPATH = "_file_path"
        const val COLUMN_STATUS = "_status"
        const val COLUMN_HEADERS = "_headers"
        const val COLUMN_DOWNLOADED_BYTES = "_written_bytes"
        const val COLUMN_FILE_SIZE = "_file_size"
        const val COLUMN_ERROR = "_error"
        const val COLUMN_PRIORITY = "_priority"
        const val INDEX_ID = 0
        const val INDEX_COLUMN_URL = 1
        const val INDEX_COLUMN_FILEPATH = 2
        const val INDEX_COLUMN_STATUS = 3
        const val INDEX_COLUMN_HEADERS = 4
        const val INDEX_COLUMN_DOWNLOADED_BYTES = 5
        const val INDEX_COLUMN_FILE_SIZE = 6
        const val INDEX_COLUMN_ERROR = 7
        const val INDEX_COLUMN_PRIORITY = 8
    }

}