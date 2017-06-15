package com.tonyodev.fetch2;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

final class DatabaseManager implements Disposable {

    private volatile boolean isDisposed;
    private final FetchDatabase db;

    static DatabaseManager newInstance(Context context, String name) {
        return new DatabaseManager(context,name);
    }

    private DatabaseManager(Context context, String name) {

        db = Room.databaseBuilder(context,
                FetchDatabase.class, name.concat(".db")).build();

        this.isDisposed = false;
    }

    void executeTransaction(final Transaction transaction) {

        try {
            transaction.onPreExecute();
            transaction.onExecute(new RealmDatabase(db));
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            transaction.onPostExecute();
        }
    }

    private static class RealmDatabase implements Database {

        private final FetchDatabase fetchDatabase;

        public RealmDatabase(FetchDatabase fetchDatabase) {
            this.fetchDatabase = fetchDatabase;
        }

        @Override
        public boolean contains(long id) {

            RequestInfo requestInfo = fetchDatabase.requestInfoDao().query(id);

            if (requestInfo == null) {
                return false;
            }

            return true;
        }

        @Override
        public boolean insert(final long id,final String url,final String absoluteFilePath) {
            if(contains(id)) {
                return false;
            }

            RequestInfo requestInfo = RequestInfo.newInstance(id,url,absoluteFilePath);
            long inserted = fetchDatabase.requestInfoDao().insert(requestInfo);

            if (inserted == -1) {
                return false;
            }

            return true;
        }

        @Override
        @NonNull
       public  List<RequestData> queryByStatus(int status) {
            List<RequestData> list = new ArrayList<>();

            List<RequestInfo> requestInfos = fetchDatabase.requestInfoDao().queryByStatus(status);

            if (requestInfos == null) {
                return list;
            }


            for (RequestInfo requestInfo : requestInfos) {
                list.add(requestInfo.toRequestData());
            }

            return list;
        }

        @Override
        @Nullable
        public RequestData query(final long id) {
            RequestData requestData = null;

            RequestInfo requestInfo = fetchDatabase.requestInfoDao().query(id);

            if (requestInfo != null) {
                requestData = requestInfo.toRequestData();
            }

            return requestData;
        }

        @Override
        @NonNull
        public List<RequestData> query() {
            List<RequestData> list = new ArrayList<>();

            List<RequestInfo> requestInfoList = fetchDatabase.requestInfoDao().query();

            if (requestInfoList == null) {
                return list;
            }

            for (RequestInfo requestInfo : requestInfoList) {
                list.add(requestInfo.toRequestData());
            }

            return list;
        }

        @Override
        @NonNull
        public List<RequestData> query(long[] ids) {
            List<RequestData> list = new ArrayList<>();

            List<RequestInfo> requestInfos = fetchDatabase.requestInfoDao().query(ids);

            if (requestInfos == null) {
             return list;
            }

            for (RequestInfo requestInfo : requestInfos) {
                list.add(requestInfo.toRequestData());
            }

            return list;
        }

        @Override
        public void updateDownloadedBytes(final long id,final long downloadedBytes) {
            fetchDatabase.requestInfoDao().updateDownloadedBytes(id,downloadedBytes);
        }

        @Override
        public void setDownloadedBytesAndTotalBytes(final long id, final long downloadedBytes, final long totalBytes){
            fetchDatabase.requestInfoDao().setDownloadedBytesAndTotalBytes(id,downloadedBytes,totalBytes);
        }

        @Override
        public void remove(final long id) {
            fetchDatabase.requestInfoDao().remove(id);
        }

        @Override
        public void setStatusAndError(final long id,final Status status, final int error) {
            fetchDatabase.requestInfoDao().setStatusAndError(id,status.getValue(),error);
        }
    };

    @Override
    public synchronized void dispose() {
        if(!isDisposed) {
            isDisposed = true;
        }
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
}