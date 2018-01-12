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
            return db.rawQuery("SELECT * FROM " + TABLE_NAME, null)
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
        val VERSION = 2
        val DB_NAME = "com_tonyodev_fetch.db"
        val TABLE_NAME = "requests"
        val COLUMN_ID = "_id"
        val COLUMN_URL = "_url"
        val COLUMN_FILEPATH = "_file_path"
        val COLUMN_STATUS = "_status"
        val COLUMN_HEADERS = "_headers"
        val COLUMN_DOWNLOADED_BYTES = "_written_bytes"
        val COLUMN_FILE_SIZE = "_file_size"
        val COLUMN_ERROR = "_error"
        val COLUMN_PRIORITY = "_priority"
        val INDEX_COLUMN_URL = 1
        val INDEX_COLUMN_FILEPATH = 2
        val INDEX_COLUMN_STATUS = 3
        val INDEX_COLUMN_HEADERS = 4
        val INDEX_COLUMN_DOWNLOADED_BYTES = 5
        val INDEX_COLUMN_FILE_SIZE = 6
        val INDEX_COLUMN_ERROR = 7
        val INDEX_COLUMN_PRIORITY = 8
    }

}