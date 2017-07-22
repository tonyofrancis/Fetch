/*
 * Copyright (C) 2017 Tonyo Francis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tonyodev.fetch;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.tonyodev.fetch.exception.EnqueueException;

/**
 * Database Helper used by Fetch and the FetchService
 * to store and manage download requests into the SQL database.
 *
 * @author Tonyo Francis
 */
final class DatabaseHelper extends SQLiteOpenHelper {

    private static final int VERSION = 2;
    private static final String DB_NAME = "com_tonyodev_fetch.db";
    private static final String TABLE_NAME = "requests";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_URL = "_url";
    private static final String COLUMN_FILEPATH = "_file_path";
    private static final String COLUMN_STATUS = "_status";
    private static final String COLUMN_HEADERS = "_headers";
    private static final String COLUMN_DOWNLOADED_BYTES = "_written_bytes";
    private static final String COLUMN_FILE_SIZE = "_file_size";
    private static final String COLUMN_ERROR = "_error";
    private static final String COLUMN_PRIORITY = "_priority";

    /*Convenience INDEXES. DO NOT USE for anything else other than extracting
     *from a cursor that has all columns. Helps with
     *faster lookup*/
    static final int INDEX_COLUMN_ID = 0;
    static final int INDEX_COLUMN_URL = 1;
    static final int INDEX_COLUMN_FILEPATH = 2;
    static final int INDEX_COLUMN_STATUS = 3;
    static final int INDEX_COLUMN_HEADERS = 4;
    static final int INDEX_COLUMN_DOWNLOADED_BYTES = 5;
    static final int INDEX_COLUMN_FILE_SIZE = 6;
    static final int INDEX_COLUMN_ERROR = 7;
    static final int INDEX_COLUMN_PRIORITY = 8;

    static final int EMPTY_COLUMN_VALUE = -1;

    private static DatabaseHelper databaseHelper;

    private final SQLiteDatabase db;
    private boolean loggingEnabled = true;

    private DatabaseHelper(Context context) {
        super(context, DB_NAME,null,VERSION);
        this.db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

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
                + "unique( " + COLUMN_FILEPATH + " ) )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1: {
                db.execSQL("CREATE UNIQUE INDEX table_unique ON "
                        + TABLE_NAME + " ( " + COLUMN_FILEPATH + ")");
                break;
            }
            default:{
            }
        }
    }

    static synchronized DatabaseHelper getInstance(Context context) {

        if(databaseHelper != null) {
            return databaseHelper;
        }

        if(context == null) {
            throw new NullPointerException("Context cannot be null");
        }

        databaseHelper = new DatabaseHelper(context.getApplicationContext());

        return databaseHelper;
    }

    synchronized boolean insert(long id, String url, String filePath, int status,
                                String headers,long downloadedBytes,long fileSize,
                                int priority, int error) {

        String statement = getInsertStatementOpen()
                 + getRowInsertStatement(id,url,filePath,status,headers, downloadedBytes,fileSize,priority,error)
                 + getInsertStatementClose();

        return insert(statement);
    }

    String getInsertStatementOpen() {
        return "INSERT INTO " + TABLE_NAME
                + " ( " +COLUMN_ID + ", " + COLUMN_URL
                + ", " + COLUMN_FILEPATH + ", " + COLUMN_STATUS
                + ", " + COLUMN_HEADERS + ", " + COLUMN_DOWNLOADED_BYTES
                + ", " + COLUMN_FILE_SIZE + ", " + COLUMN_ERROR
                + ", " + COLUMN_PRIORITY + " ) VALUES ";
    }

    String getRowInsertStatement(long id, String url, String filePath, int status,
                                 String headers, long downloadedBytes, long fileSize,
                                 int priority, int error) {
        return "( "
                + id
                + ", " + DatabaseUtils.sqlEscapeString(url)
                + ", " + DatabaseUtils.sqlEscapeString(filePath)
                + ", " + status
                + ", " + DatabaseUtils.sqlEscapeString(headers)
                + ", " + downloadedBytes
                + ", " + fileSize
                + ", " + error
                + ", " + priority +" )";
    }

    String getInsertStatementClose() {
        return ";";
    }

    synchronized boolean insert(String insertStatement) {

        boolean inserted = false;

        if(insertStatement == null) {
            return inserted;
        }

        try {

            db.beginTransaction();
            db.execSQL(insertStatement);
            db.setTransactionSuccessful();
        }catch (Exception e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }

            throw new EnqueueException(e.getMessage(),ErrorUtils.getCode(e.getMessage()));
        }finally {
            try {
                db.endTransaction();
                inserted = true;
            }catch (SQLiteException e) {

                if(loggingEnabled) {
                    e.printStackTrace();
                }

                throw new EnqueueException(e.getMessage(),ErrorUtils.getCode(e.getMessage()));
            }
        }

        return inserted;
    }

    synchronized boolean pause(long id) {

        boolean paused = false;

        try {

            db.beginTransaction();
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_STATUS + " = "
                    + FetchConst.STATUS_PAUSED + " WHERE " + COLUMN_ID + " = " + id
                    + " AND " + COLUMN_STATUS + " != " + FetchConst.STATUS_DONE
                    + " AND " + COLUMN_STATUS + " != " + FetchConst.STATUS_ERROR);

            db.setTransactionSuccessful();
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        Cursor cursor = null;
        try {

            db.endTransaction();

            cursor = db.rawQuery("SELECT " + COLUMN_ID
                    + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = " + id
                    + " AND " + COLUMN_STATUS + " = " + FetchConst.STATUS_PAUSED,null);

            if(cursor != null && cursor.getCount() > 0) {
                paused = true;
            }

        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return paused;
    }

    synchronized boolean resume(long id) {

        boolean resumed = false;

        try {
            db.beginTransaction();
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_STATUS + " = "
                    + FetchConst.STATUS_QUEUED + " WHERE " + COLUMN_ID + " = " + id
                    + " AND " + COLUMN_STATUS + " != " + FetchConst.STATUS_DONE
                    + " AND " + COLUMN_STATUS + " != " + FetchConst.STATUS_ERROR);

            db.setTransactionSuccessful();
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        Cursor cursor = null;
        try {
            db.endTransaction();

            cursor = db.rawQuery("SELECT " + COLUMN_ID
                    + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = " + id
                    + " AND " + COLUMN_STATUS + " = " + FetchConst.STATUS_QUEUED,null);

            if(cursor != null && cursor.getCount() > 0) {
                resumed = true;
            }

        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return resumed;
    }

    synchronized boolean updateStatus(long id, int status, int error) {

        boolean updated = false;

        try {
            db.beginTransaction();
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_STATUS + " = "
                    + status + ", " + COLUMN_ERROR + " = " + error + " WHERE "
                    + COLUMN_ID + " = " + id);

            db.setTransactionSuccessful();
        }catch (SQLiteException e){

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        try {
            db.endTransaction();
            updated = true;
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        return updated;
    }

    synchronized boolean updateFileBytes(long id, long downloadedBytes, long fileSize) {

        boolean updated = false;

        try {
            db.beginTransaction();
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_FILE_SIZE + " = "
                    + fileSize + ", " + COLUMN_DOWNLOADED_BYTES + " = " + downloadedBytes
                    + " WHERE " + COLUMN_ID + " = " + id);

            db.setTransactionSuccessful();
        }catch (SQLiteException e){

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        try {
            db.endTransaction();
            updated = true;
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        return updated;
    }

    synchronized boolean delete(long id) {

        boolean removed = false;

        try {
            db.beginTransaction();
            db.execSQL("DELETE FROM " + TABLE_NAME
                    + " WHERE " + COLUMN_ID + " = " + id);

            db.setTransactionSuccessful();
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        try {
            db.endTransaction();
            removed = true;
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        return removed;
    }

    synchronized boolean deleteAll() {

        boolean removed = false;

        try {
            db.beginTransaction();
            db.execSQL("DELETE FROM " + TABLE_NAME);

            db.setTransactionSuccessful();
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        try {
            db.endTransaction();
            removed = true;
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        return removed;
    }

    synchronized boolean setPriority(long id, int priority) {

        boolean updated = false;

        try {

            db.beginTransaction();
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_PRIORITY + " = "
                    + priority + " WHERE " + COLUMN_ID + " = " + id);

            db.setTransactionSuccessful();
        }catch (SQLiteException e){

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        try {
            db.endTransaction();
            updated = true;
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        return updated;
    }

    synchronized boolean retry(long id) {

        boolean updated = false;

        try {
            db.beginTransaction();
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_STATUS + " = "
                    + FetchConst.STATUS_QUEUED + ", " + COLUMN_ERROR + " = "
                    + EMPTY_COLUMN_VALUE + " WHERE " + COLUMN_ID + " = " + id
                    + " AND " + COLUMN_STATUS + " = " + FetchConst.STATUS_ERROR);

            db.setTransactionSuccessful();
        }catch (SQLiteException e){

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        Cursor cursor = null;
        try {
            db.endTransaction();

            cursor = db.rawQuery("SELECT " + COLUMN_ID
                    + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = " + id
                    + " AND " + COLUMN_STATUS + " = " + FetchConst.STATUS_QUEUED,null);

            if(cursor != null && cursor.getCount() > 0) {
                updated = true;
            }

        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return updated;
    }

    synchronized boolean updateUrl(long id,String url) {

        boolean updated = false;

        try {
            db.beginTransaction();
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_URL + " = "
                    + DatabaseUtils.sqlEscapeString(url) + " WHERE " + COLUMN_ID + " = " + id);

            db.setTransactionSuccessful();
        }catch (SQLiteException e){

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        Cursor cursor = null;
        try {
            db.endTransaction();

            cursor = db.rawQuery("SELECT " + COLUMN_ID
                    + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = " + id
                    + " AND " + COLUMN_URL + " = " + DatabaseUtils.sqlEscapeString(url),null);

            if(cursor != null && cursor.getCount() > 0) {
                updated = true;
            }

        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return updated;
    }

    synchronized Cursor get(long id) {

        try {
            return db.rawQuery("SELECT * FROM " + TABLE_NAME
                    + " WHERE " + COLUMN_ID + " = " + id,null);
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
            return null;
        }
    }

    synchronized Cursor get() {

        try {
            return db.rawQuery("SELECT * FROM " + TABLE_NAME,null);
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
            return null;
        }
    }

    synchronized Cursor get(long[] ids) {

        try {

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('(');

            if(ids.length > 0) {
                for (long id : ids) {
                    stringBuilder.append(id)
                            .append(',');
                }

                stringBuilder.deleteCharAt(stringBuilder.length()-1);
            }

            stringBuilder.append(')');


            return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID
                    + " IN " + stringBuilder.toString(),null);
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
            return null;
        }
    }

    synchronized Cursor getByStatus(int status) {

        try {
            return db.rawQuery("SELECT * FROM " + TABLE_NAME
                    + " WHERE " + COLUMN_STATUS + " = " + status,null);
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
            return null;
        }
    }

    synchronized Cursor getByUrlAndFilePath(String url,String filePath) {

        try {
            return db.rawQuery("SELECT * FROM " + TABLE_NAME
                    + " WHERE " + COLUMN_URL + " = "
                    + DatabaseUtils.sqlEscapeString(url) + " AND " + COLUMN_FILEPATH
                    + " = " + DatabaseUtils.sqlEscapeString(filePath) + " LIMIT 1",null);
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
            return null;
        }
    }

    synchronized Cursor getNextPendingRequest() {

        Cursor cursor = db.rawQuery("SELECT * FROM "
                + TABLE_NAME + " WHERE " + COLUMN_STATUS + " = "
                + FetchConst.STATUS_QUEUED + " AND "
                + COLUMN_PRIORITY + " = " + FetchConst.PRIORITY_HIGH
                + " LIMIT 1" ,null);

        if(cursor != null && cursor.getCount() > 0) {
            return cursor;
        }

        if(cursor != null) {
            cursor.close();
        }

        return db.rawQuery("SELECT * FROM "
                + TABLE_NAME + " WHERE " + COLUMN_STATUS + " = "
                + FetchConst.STATUS_QUEUED + " LIMIT 1" ,null);
    }

    synchronized boolean hasPendingRequests() {

        Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + " FROM "
                + TABLE_NAME + " WHERE " + COLUMN_STATUS + " = "
                + FetchConst.STATUS_QUEUED + " LIMIT 1" ,null);

        boolean hasPending = false;

        if(cursor != null && cursor.getCount() > 0) {
            hasPending = true;
        }

        if(cursor != null) {
            cursor.close();
        }

        return hasPending;
    }

    synchronized void verifyOK() {

        try {

            db.beginTransaction();
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_STATUS + " = "
                    + FetchConst.STATUS_QUEUED + " WHERE " + COLUMN_STATUS
                    + " = " + FetchConst.STATUS_DOWNLOADING);

            db.setTransactionSuccessful();

        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        try {
            db.endTransaction();
        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }
    }

    synchronized void clean() {

        Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + ", " + COLUMN_FILEPATH
                + " FROM " + TABLE_NAME + " WHERE " + COLUMN_STATUS + " = "
                + FetchConst.STATUS_DONE ,null);

        if(cursor == null) {
            return;
        }

        if(cursor.getCount() < 1) {
            cursor.close();
            return;
        }

        try {

            db.beginTransaction();

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {

                String destinationUri = cursor.getString(cursor.getColumnIndex(COLUMN_FILEPATH));

                if(destinationUri != null) {

                    if(!Utils.fileExist(destinationUri)) {
                        long id = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
                        db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_STATUS + " = "
                                + FetchConst.STATUS_ERROR + ", " + COLUMN_ERROR + " = "
                                + FetchConst.ERROR_FILE_NOT_FOUND + " WHERE "
                                + COLUMN_ID + " = " + id);
                    }
                }

                cursor.moveToNext();
            }

            db.setTransactionSuccessful();

        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        try {
            db.endTransaction();

        }catch (SQLiteException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }

        } finally {
            cursor.close();
        }
    }

    synchronized void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
    }
}