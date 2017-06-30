package com.tonyodev.fetch2.util;

import android.content.Context;
import android.net.Uri;

import com.tonyodev.fetch2.Callback;
import com.tonyodev.fetch2.Query;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.Status;

import java.util.List;

import okhttp3.OkHttpClient;

public final class Assert {

    private Assert() {}

    public static void contextNotNull(Context context) {

        if(context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
    }

    public static void groupIDIsNotNull(String groupId) {

        if (groupId == null) {
            throw new IllegalArgumentException("groupId cannot be null");
        }
    }

    public static void requestIsNotNull(Request request) {

        if(request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
    }

    public static void clientIsNotNull(OkHttpClient client) {

        if(client == null) {
            throw new IllegalArgumentException("OkHttpClient cannot be null");
        }
    }

    public static void statusIsNotNull(Status status) {

        if(status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    public static void requestListIsNotNull(List<Request> list) {

        if(list == null) {
            throw new IllegalArgumentException("List<Request> cannot be null");
        }
    }

    public static void idListIsNotNull(List<Long> list) {

        if(list== null) {
            throw new IllegalArgumentException("List<Long> cannot be null");
        }
    }

    public static void callbackIsNotNull(Callback callback) {

        if(callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
    }

    public static void queryIsNotNull(Query query) {

        if(query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
    }

    public static void urlIsNotNullOrEmpty(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Url cannot be null or empty");
        }
    }

    public static void filePathIsNotNull(String filePath) {
        if(filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("file path cannot be null or empty");
        }
    }

    public static void keyIsNotNullOrEmpty(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
    }

    public static void validUriSchema(String url) {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalArgumentException("Can only enqueue HTTP/HTTPS URIs: " + uri);
        }
    }
}
