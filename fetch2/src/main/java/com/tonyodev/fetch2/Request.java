package com.tonyodev.fetch2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import java.util.Map;


public final class Request {

    private final long id;
    private final String url;
    private final String absoluteFilePath;
    private final Map<String,String> headers;

    public Request(@NonNull String url, @NonNull String absoluteFilePath) {
        this(url,absoluteFilePath,null);
    }

    public Request(@NonNull String url, @NonNull String absoluteFilePath,@Nullable Map<String,String> headers) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Url cannot be null or empty");
        }

        if(absoluteFilePath == null || absoluteFilePath.isEmpty()) {
            throw new IllegalArgumentException("AbsoluteFilePath cannot be null or empty");
        }

        if(headers == null) {
            headers = new ArrayMap<>();
        }

        this.url = url;
        this.absoluteFilePath = absoluteFilePath;
        this.headers = headers;
        this.id = generateId();
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @NonNull
    public String getAbsoluteFilePath() {
        return absoluteFilePath;
    }

    @NonNull
    public Map<String,String> getHeaders() {
        return headers;
    }

    public void putHeader(@NonNull String key, @Nullable String value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        if(value == null) value = "";

        headers.put(key,value);
    }

    private long generateId() {
        long code1 = 0;
        long code2 = 0;

        for (char c : url.toCharArray()) {
            code1 = code1 * 31 + c;
        }

        for (char c : absoluteFilePath.toCharArray()) {
            code2 = code2 * 31 + c;
        }

        return Math.abs(code1+code2);
    }

    @Override
    public String toString() {
        return "{\"url\":\"" + url + "\",\"absolutePath\":" + absoluteFilePath +"\"}";
    }
}
