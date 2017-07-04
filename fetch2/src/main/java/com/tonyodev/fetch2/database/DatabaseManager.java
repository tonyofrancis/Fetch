package com.tonyodev.fetch2.database;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.List;

public final class DatabaseManager implements Database {

    public static final String DATABASE_NAME = "com_tonyodev_fetch2.db";

    private final FetchDatabase fetchDatabase;

    public DatabaseManager(Context context) {
        this.fetchDatabase = Room.databaseBuilder(context, FetchDatabase.class, DATABASE_NAME).build();
    }

    @Override
    public ReadDatabase getReadOnlyDatabase() {
        return this;
    }

    @Override
    public WriteDatabase getWriteOnlyDatabase() {
        return this;
    }

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
    public List<DatabaseRow> queryByStatus(int status) {
        return fetchDatabase.fetchDao().queryByStatus(status);
    }

    @Override
    @Nullable
    public DatabaseRow query(final long id) {
        return fetchDatabase.fetchDao().query(id);
    }

    @Override
    @NonNull
    public List<DatabaseRow> query() {
        return fetchDatabase.fetchDao().query();
    }

    @Override
    @NonNull
    public List<DatabaseRow> query(long[] ids) {
        return fetchDatabase.fetchDao().query(ids);
    }

    @NonNull
    @Override
    public List<DatabaseRow> queryByGroupId(String groupId) {
        return fetchDatabase.fetchDao().queryByGroupId(groupId);
    }

    @NonNull
    @Override
    public List<DatabaseRow> queryGroupByStatusId(String groupId,int status) {
        return fetchDatabase.fetchDao().queryGroupByStatusId(groupId, status);
    }

    @Override
    public boolean remove(final long id) {
        long rows = fetchDatabase.fetchDao().remove(id);
        return rows > 0;
    }

    @Override
    public void removeAll() {
        fetchDatabase.fetchDao().removeAll();
    }

    @Override
    public void remove(long[] ids) {
        fetchDatabase.fetchDao().remove(ids);
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
    public void setStatusAndError(final long id,final int status, final int error) {
        fetchDatabase.fetchDao().setStatusAndError(id,status,error);
    }

    @Override
    public void close() throws IOException {
        fetchDatabase.close();
    }
}