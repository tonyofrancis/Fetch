package com.tonyodev.fetch2.database;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.tonyodev.fetch2.RequestData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseManager {

    public static final String DEFAULT_DATABASE_NAME = "com_tonyodev_fetch.db";

    private final FetchDatabase fetchDatabase;

    public DatabaseManager(Context context) {
        this.fetchDatabase = Room.databaseBuilder(context, FetchDatabase.class, DEFAULT_DATABASE_NAME).build();
    }

    public synchronized void executeTransaction(Transaction transaction) {
        transaction.onExecute(database);
    }

    public ReadDatabase getReadDatabase() {
        return database;
    }

    public WriteDatabase getWriteDatabase() {
        return database;
    }

    private final Database database = new Database() {

        @Override
        public boolean contains(long id) {
            DatabaseRow databaseRow = fetchDatabase.fetchDao().query(id);
            return databaseRow != null;
        }

        @Override
        public boolean insert(DatabaseRow databaseRow) {
            long inserted = fetchDatabase.fetchDao().insert(databaseRow);
            return inserted != -1;
        }

        @Override
        public void insert(List<DatabaseRow> databaseRows) {
            fetchDatabase.fetchDao().insert(databaseRows);
        }

        @Override
        @NonNull
        public List<RequestData> queryByStatus(int status) {
            List<RequestData> list = new ArrayList<>();
            List<DatabaseRow> databaseRows = fetchDatabase.fetchDao().queryByStatus(status);

            if (databaseRows == null) {
                return list;
            }

            for (DatabaseRow databaseRow : databaseRows) {
                list.add(databaseRow.toRequestData());
            }

            return list;
        }

        @Override
        @Nullable
        public RequestData query(final long id) {
            RequestData requestData = null;
            DatabaseRow databaseRow = fetchDatabase.fetchDao().query(id);

            if (databaseRow != null) {
                requestData = databaseRow.toRequestData();
            }

            return requestData;
        }

        @Override
        @NonNull
        public List<RequestData> query() {
            List<RequestData> list = new ArrayList<>();
            List<DatabaseRow> databaseRowList = fetchDatabase.fetchDao().query();

            if (databaseRowList == null) {
                return list;
            }

            for (DatabaseRow databaseRow : databaseRowList) {
                list.add(databaseRow.toRequestData());
            }

            return list;
        }

        @Override
        @NonNull
        public List<RequestData> query(long[] ids) {
            List<RequestData> list = new ArrayList<>();
            List<DatabaseRow> databaseRows = fetchDatabase.fetchDao().query(ids);

            if (databaseRows == null) {
                return list;
            }

            for (DatabaseRow databaseRow : databaseRows) {
                list.add(databaseRow.toRequestData());
            }

            return list;
        }

        @NonNull
        @Override
        public List<RequestData> queryByGroupId(String groupId) {
            List<RequestData> list = new ArrayList<>();
            List<DatabaseRow> databaseRows = fetchDatabase.fetchDao().queryByGroupId(groupId);

            if (databaseRows == null) {
                return list;
            }

            for (DatabaseRow databaseRow : databaseRows) {
                list.add(databaseRow.toRequestData());
            }

            return list;
        }

        @NonNull
        @Override
        public List<RequestData> queryGroupByStatusId(String groupId,int status) {
            List<RequestData> list = new ArrayList<>();
            List<DatabaseRow> databaseRows = fetchDatabase.fetchDao().queryGroupByStatusId(groupId, status);

            if (databaseRows == null) {
                return list;
            }

            for (DatabaseRow databaseRow : databaseRows) {
                list.add(databaseRow.toRequestData());
            }

            return list;
        }

        @Override
        public void updateDownloadedBytes(final long id,final long downloadedBytes) {
            fetchDatabase.fetchDao().updateDownloadedBytes(id,downloadedBytes);
        }

        @Override
        public void setDownloadedBytesAndTotalBytes(final long id, final long downloadedBytes, final long totalBytes){
            fetchDatabase.fetchDao().setDownloadedBytesAndTotalBytes(id,downloadedBytes,totalBytes);
        }

        @Override
        public boolean remove(final long id) {
            long rows = fetchDatabase.fetchDao().remove(id);
            return rows > 0;
        }

        @Override
        public void setStatusAndError(final long id,final int status, final int error) {
            fetchDatabase.fetchDao().setStatusAndError(id,status,error);
        }

        @Override
        public void close() throws IOException {
            fetchDatabase.close();
        }
    };
}