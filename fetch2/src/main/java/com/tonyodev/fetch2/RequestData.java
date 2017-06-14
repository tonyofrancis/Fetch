package com.tonyodev.fetch2;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import java.util.Map;

public final class RequestData {

    private final String url;
    private final String absoluteFilePath;
    private final Status status;
    private final Error error;
    private final long downloadedBytes;
    private final long totalBytes;
    private final int progress;
    private final Map<String,String> headers;
    private final Request request;

    public RequestData(@NonNull String url,@NonNull String absoluteFilePath, int status,
                       int error, long downloadedBytes, long totalBytes,@NonNull Map<String,String> headers) {

        if (url == null) {
            throw new IllegalArgumentException("Url cannot be null");
        }

        if(absoluteFilePath == null) {
            throw new IllegalArgumentException("AbsoluteFilePath cannot be null");
        }

        if (headers == null) {
            headers = new ArrayMap<>();
        }

        this.url = url;
        this.absoluteFilePath = absoluteFilePath;
        this.downloadedBytes = downloadedBytes;
        this.totalBytes = totalBytes;
        this.error = Error.valueOf(error);
        this.status = Status.valueOf(status);
        this.progress = DownloadHelper.calculateProgress(downloadedBytes,totalBytes);
        this.headers = headers;
        this.request = new Request(url,absoluteFilePath,headers);
    }

    public long getId() {
        return request.getId();
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
    public Map<String, String> getHeaders() {
        return headers;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @NonNull
    public Error getError() {
        return error;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getProgress() {
        return progress;
    }

    @NonNull
    public Request getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return request.toString();
    }
}
