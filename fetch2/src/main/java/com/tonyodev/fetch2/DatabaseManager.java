package com.tonyodev.fetch2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

final class DatabaseManager implements Disposable {

    private final String name;
    private final RealmConfiguration realmConfiguration;
    private volatile boolean isDisposed;

    static class LookUpField {
        static final String ID = "id";
        static final String STATUS = "status";
    }

    static DatabaseManager newInstance(Context context, String name) {
        return new DatabaseManager(context,name);
    }

    private DatabaseManager(Context context, String name) {
        Realm.init(context);
        this.isDisposed = false;
        this.name = getRealmFileName(name);
        this.realmConfiguration = getRealmConfiguration();
    }

    private String getRealmFileName(String name) {
        return name.concat(".realm");
    }

    private RealmConfiguration getRealmConfiguration() {
        return new RealmConfiguration.Builder()
                .name(name)
                .modules(new DatabaseModule())
                .schemaVersion(1)
                .build();
    }

    private Realm newRealmInstance() {
       return Realm.getInstance(realmConfiguration);
    }

    void executeTransaction(final Transaction transaction) {
        Realm realm = null;

        try {
            realm = newRealmInstance();
            transaction.onPreExecute();
            realm.beginTransaction();
            transaction.onExecute(new RealmDatabase(realm));
            realm.commitTransaction();
        }catch (Exception e) {
            e.printStackTrace();

            if (realm != null && realm.isInTransaction()) {
                realm.cancelTransaction();
            }
        }finally {
            if(realm != null && !realm.isClosed()) {
                realm.close();
            }
            transaction.onPostExecute();
        }
    }

    private static class RealmDatabase implements Database {

        private final Realm realm;

        public RealmDatabase(Realm realm) {
            this.realm = realm;
        }

        @Override
        public boolean contains(long id) {

            long count = realm.where(RealmRequestInfo.class)
                    .equalTo(LookUpField.ID,id)
                    .count();

            return count > 0;
        }

        @Override
        public boolean insert(final long id,final String url,final String absoluteFilePath) {
            if(contains(id)) {
                return false;
            }

            realm.copyToRealm(RealmRequestInfo.newInstance(id,url,absoluteFilePath));

            return true;
        }

        @Override
        @NonNull
       public  List<RequestData> queryByStatus(int status) {
            List<RequestData> list = new ArrayList<>();

            RealmResults<RealmRequestInfo> results = realm.where(RealmRequestInfo.class)
                    .equalTo(LookUpField.STATUS,status)
                    .findAll();

            for (RealmRequestInfo result : results) {
                list.add(result.toRequestData());
            }

            return list;
        }

        @Override
        @Nullable
        public RequestData query(final long id) {
            RequestData requestData = null;

            RealmRequestInfo realmRequestInfo = realm.where(RealmRequestInfo.class)
                    .equalTo(LookUpField.ID,id)
                    .findFirst();

            if (realmRequestInfo != null) {
                requestData = realmRequestInfo.toRequestData();
            }

            return requestData;
        }

        @Override
        @NonNull
        public List<RequestData> query() {
            List<RequestData> list = new ArrayList<>();

            RealmResults<RealmRequestInfo> results = realm.where(RealmRequestInfo.class).findAll();

            for (RealmRequestInfo result : results) {
                list.add(result.toRequestData());
            }

            return list;
        }

        @Override
        @NonNull
        public List<RequestData> query(Long[] ids) {
            List<RequestData> list = new ArrayList<>();

            RealmResults<RealmRequestInfo> results = realm.where(RealmRequestInfo.class)
                    .in(LookUpField.ID,ids)
                    .findAll();

            for (RealmRequestInfo result : results) {
                list.add(result.toRequestData());
            }

            return list;
        }

        @Override
        public void updateDownloadedBytes(final long id,final long downloadedBytes) {

            RealmRequestInfo realmRequestInfo = realm.where(RealmRequestInfo.class)
                    .equalTo(LookUpField.ID,id)
                    .findFirst();

            if(realmRequestInfo != null) {
                realmRequestInfo.setDownloadedBytes(downloadedBytes);
            }
        }

        @Override
        public void setDownloadedBytesAndTotalBytes(final long id, final long downloadedBytes, final long totalBytes){

            RealmRequestInfo realmRequestInfo = realm.where(RealmRequestInfo.class)
                    .equalTo(LookUpField.ID,id)
                    .findFirst();

            if(realmRequestInfo != null){
                realmRequestInfo.setDownloadedBytes(downloadedBytes);
                realmRequestInfo.setTotalBytes(totalBytes);
            }
        }

        @Override
        public void remove(final long id) {
            RealmRequestInfo realmRequestInfo = realm.where(RealmRequestInfo.class)
                    .equalTo(LookUpField.ID,id)
                    .findFirst();

            if(realmRequestInfo != null) {
                realmRequestInfo.deleteFromRealm();
            }
        }

        @Override
        public void setStatusAndError(final long id,final Status status, final int error) {

            RealmRequestInfo realmRequestInfo = realm.where(RealmRequestInfo.class)
                    .equalTo(LookUpField.ID,id)
                    .findFirst();

            if(realmRequestInfo != null) {
                realmRequestInfo.setStatus(status.getValue());
                realmRequestInfo.setError(error);
            }
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