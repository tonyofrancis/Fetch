package com.tonyodev.fetch2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;

import com.tonyodev.fetch2.util.Assert;

import java.util.Map;


public final class Request {

    public static final String DEFAULT_GROUP_ID = "fetch_group";

    private final long id;
    private final String url;
    private final String absoluteFilePath;
    private final Map<String,String> headers;
    private String groupId;

    public Request(@NonNull String url, @NonNull String absoluteFilePath) {
        this(url,absoluteFilePath,null);
    }

    public Request(@NonNull String url, @NonNull String absoluteFilePath,@Nullable Map<String,String> headers) {
        Assert.urlIsNotNullOrEmpty(url);
        Assert.validUriSchema(url);
        Assert.filePathIsNotNull(absoluteFilePath);

        if(headers == null) {
            headers = new ArrayMap<>();
        }

        this.groupId = DEFAULT_GROUP_ID;
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
        Assert.keyIsNotNullOrEmpty(key);

        if(value == null) {
            value = "";
        }

        headers.put(key,value);
    }

    @NonNull
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(@NonNull String groupId) {
        Assert.groupIDIsNotNull(groupId);
        this.groupId = groupId;
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

    @Override
    public int hashCode() {
        return (int)id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Request) {
            Request request = (Request)obj;
            if (id == request.getId()) {
                return true;
            }
        }

        return false;
    }
}
