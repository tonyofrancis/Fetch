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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.tonyodev.fetch.exception.EnqueueException;

import java.util.ArrayList;
import java.util.List;

/**
 * Database Helper used by Fetch and the FetchService
 * to store and manage download requests into the SQL database.
 *
 * @author Tonyo Francis
 */
final class DatabaseHelper extends SQLiteOpenHelper {

    private static final int VERSION = 1;
    private static final String DB_NAME = "com_tonyodev_fetch.db";
    private static final String TABLE_NAME = "requests";

    static final String COLUMN_ID = "_id";
    static final String COLUMN_URL = "_url";
    static final String COLUMN_FILEPATH = "_file_path";
    static final String COLUMN_STATUS = "_status";
    static final String COLUMN_HEADERS = "_headers";
    static final String COLUMN_WRITTEN_BYTES = "_written_bytes";
    static final String COLUMN_FILE_SIZE = "_file_size";
    static final String COLUMN_ERROR = "_error";
    static final String COLUMN_PRIORITY = "_priority";

    static final int INDEX_COLUMN_ID = 0;
    static final int INDEX_COLUMN_URL = 1;
    static final int INDEX_COLUMN_FILEPATH = 2;
    static final int INDEX_COLUMN_STATUS = 3;
    static final int INDEX_COLUMN_HEADERS = 4;
    static final int INDEX_COLUMN_WRITTEN_BYTES = 5;
    static final int INDEX_COLUMN_FILE_SIZE = 6;
    static final int INDEX_COLUMN_ERROR = 7;
    static final int INDEX_COLUMN_PRIORITY = 8;

    static final int EMPTY_COLUMN_VALUE = -1;

    private static DatabaseHelper databaseHelper;

    private final SQLiteDatabase db;

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
                + COLUMN_WRITTEN_BYTES + " INTEGER NOT NULL, "
                + COLUMN_FILE_SIZE + " INTEGER NOT NULL, "
                + COLUMN_ERROR + " INTEGER NOT NULL, "
                + COLUMN_PRIORITY + " INTEGER NOT NULL )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

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

    private synchronized boolean containsFilePath(String filePath) {

        boolean found = false;

        try {

            Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID
                    + " FROM " + TABLE_NAME + " WHERE " + COLUMN_FILEPATH + " LIKE '%"
                    + filePath + "%'",null);


            if(cursor != null && cursor.getCount() > 0) {
                found = true;
                cursor.close();
            }

        }catch (SQLiteException e) {
            found = true;
        }

        return found;
    }

    String getInsertStatement(long id, String url, String filePath, int status,
                                      String headers,long writtenBytes,long fileSize,
                                      int priority, int error) throws EnqueueException {


        if(containsFilePath(filePath)) {

            throw new EnqueueException("DatabaseHelper already containsFilePath a request with the filePath:"
                    + filePath,FetchConst.ERROR_REQUEST_ALREADY_EXIST);
        }

        return "INSERT INTO " + TABLE_NAME + " VALUES ( " + id
                + ", '" + url
                + "', '" + filePath
                + "', " + status
                + ", '" + headers
                + "', " + writtenBytes
                + ", " + fileSize
                + ", " + error
                + ", " + priority +" )";
    }

    synchronized boolean insert(long id, String url, String filePath, int status,
                                String headers,long writtenBytes,long fileSize,
                                int priority, int error) {

        String statement = getInsertStatement(id,url,filePath,status,headers,
                writtenBytes,fileSize,priority,error);

        List<String> insertStatements = new ArrayList<>(1);
        insertStatements.add(statement);

        return insert(insertStatements);
    }

    synchronized boolean insert(List<String> statements) {

        boolean inserted = false;

        if(statements == null) {
            return inserted;
        }

        try {

            db.beginTransaction();

            for (String statement : statements) {
                db.execSQL(statement);
            }

            db.setTransactionSuccessful();
        }catch (Exception e) {
            throw new EnqueueException(e.getMessage(),ErrorUtils.getCode(e.getMessage()));
        }

        try {
            db.endTransaction();
            inserted = true;
        }catch (SQLiteException e) {
            throw new EnqueueException(e.getMessage(),ErrorUtils.getCode(e.getMessage()));
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
            e.printStackTrace();
        }

        try {

            db.endTransaction();

            Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID
                    + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = " + id
                    + " AND " + COLUMN_STATUS + " = " + FetchConst.STATUS_PAUSED,null);

            if(cursor != null && cursor.getCount() > 0) {
                paused = true;
                cursor.close();
            }

        }catch (SQLiteException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }

        try {
            db.endTransaction();

            Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID
                    + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = " + id
                    + " AND " + COLUMN_STATUS + " = " + FetchConst.STATUS_QUEUED,null);

            if(cursor != null && cursor.getCount() > 0) {
                resumed = true;
                cursor.close();
            }

        }catch (SQLiteException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }

        try {
            db.endTransaction();
            updated = true;
        }catch (SQLiteException e) {
            e.printStackTrace();
        }

        return updated;
    }

    synchronized boolean updateFileBytes(long id, long writtenBytes, long fileSize) {

        boolean updated = false;

        try {
            db.beginTransaction();
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_FILE_SIZE + " = "
                    + fileSize + ", " + COLUMN_WRITTEN_BYTES + " = " + writtenBytes
                    + " WHERE " + COLUMN_ID + " = " + id);

            db.setTransactionSuccessful();
        }catch (SQLiteException e){
            e.printStackTrace();
        }

        try {
            db.endTransaction();
            updated = true;
        }catch (SQLiteException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }

        try {
            db.endTransaction();
            removed = true;
        }catch (SQLiteException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }

        try {
            db.endTransaction();
            updated = true;
        }catch (SQLiteException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }

        try {
            db.endTransaction();

            Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID
                    + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = " + id
                    + " AND " + COLUMN_STATUS + " = " + FetchConst.STATUS_QUEUED,null);

            if(cursor != null && cursor.getCount() > 0) {
                updated = true;
                cursor.close();
            }

        }catch (SQLiteException e) {
            e.printStackTrace();
        }

        return updated;
    }

    synchronized Cursor get(long id) {

        try {
            return db.rawQuery("SELECT * FROM " + TABLE_NAME
                    + " WHERE " + COLUMN_ID + " = " + id,null);
        }catch (SQLiteException e) {
            return null;
        }
    }

    synchronized Cursor get() {

        try {
            return db.rawQuery("SELECT * FROM " + TABLE_NAME,null);
        }catch (SQLiteException e) {
            return null;
        }
    }

    synchronized Cursor getByStatus(int status) {

        try {
            return db.rawQuery("SELECT * FROM " + TABLE_NAME
                    + " WHERE " + COLUMN_STATUS + " = " + status,null);
        }catch (SQLiteException e) {
            return null;
        }
    }

    synchronized Cursor getNextPending() {

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

    synchronized void verifyOK() {

        try {

            db.beginTransaction();
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMN_STATUS + " = "
                    + FetchConst.STATUS_QUEUED + " WHERE " + COLUMN_STATUS
                    + " = " + FetchConst.STATUS_DOWNLOADING);

            db.setTransactionSuccessful();

        }catch (SQLiteException e) {
            e.printStackTrace();
        }

        try {
            db.endTransaction();
        }catch (SQLiteException e) {
            e.printStackTrace();
        }

        clean();
    }

    private void clean() {

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
            e.printStackTrace();
        }

        try {
            db.endTransaction();

        }catch (SQLiteException e) {
            e.printStackTrace();

        } finally {
            cursor.close();
        }
    }
}