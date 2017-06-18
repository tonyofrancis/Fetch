package com.tonyodev.fetch2;

import android.content.Context;

import java.util.IllegalFormatCodePointException;
import java.util.List;

import okhttp3.OkHttpClient;

final class FetchHelper {

    private FetchHelper() {}

    static void throwIfContextIsNull(Context context) {

        if(context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
    }

    static String getDefaultDatabaseName(){
        return "com_tonyodev_fetch";
    }


    static void throwIfFetchNameIsNullOrEmpty(String databaseName) {

        if(databaseName == null || databaseName.isEmpty()) {
            throw new IllegalArgumentException("DatabaseManager Name cannot be null or empty");
        }
    }

    static void throwIfGroupIDIsNull(String groupId) {

        if (groupId == null) {
            throw new IllegalArgumentException("groupId cannot be null");
        }
    }

    static void throwIfRequestIsNull(Request request) {

        if(request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
    }

    static void throwIfClientIsNull(OkHttpClient client) {

        if(client == null) {
            throw new IllegalArgumentException("OkHttpClient cannot be null");
        }
    }

    static void throwIfStatusIsNull(Status status) {

        if(status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    static void throwIfRequestListIsNull(List<Request> list) {

        if(list == null) {
            throw new IllegalArgumentException("List<Request> cannot be null");
        }
    }

    static void throwIfIdListIsNull(List<Long> list) {

        if(list== null) {
            throw new IllegalArgumentException("List<Long> cannot be null");
        }
    }

    static void throwIfCallbackIsNull(Callback callback) {

        if(callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
    }

    static void throwIfQueryIsNull(Query query) {

        if(query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
    }

    static void throwIfDisposed(Disposable disposable) {

        if(disposable.isDisposed()) {
            throw new DisposedException("This instance cannot be reused after disposed is called");
        }
    }

    static long[] createIdArray(List<Long> ids) {

        for (Long id : ids) {
            if(id == null) {
                throw new NullPointerException("id inside List<Long> cannot be null");
            }
        }

        long[] idArray = new long[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            idArray[i] = ids.get(i);
        }

        return idArray;
    }
}
