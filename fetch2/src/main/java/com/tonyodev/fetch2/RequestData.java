package com.tonyodev.fetch2;

import android.support.annotation.NonNull;

import java.util.Map;

public final class RequestData {

    private final Status status;
    private final Error error;
    private final long downloadedBytes;
    private final long totalBytes;
    private final int progress;
    private final Request request;

    public RequestData(Request request, Status status, Error error,long downloadedBytes, long totalBytes, int progress) {
        this.request = request;
        this.status = status;
        this.error = error;
        this.downloadedBytes = downloadedBytes;
        this.totalBytes = totalBytes;
        this.progress = progress;
    }

    public long getId() {
        return request.getId();
    }

    @NonNull
    public String getUrl() {
        return request.getUrl();
    }

    @NonNull
    public String getAbsoluteFilePath() {
        return request.getAbsoluteFilePath();
    }

    @NonNull
    public Map<String, String> getHeaders() {
        return request.getHeaders();
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

    @NonNull
    public String getGroupId() {
        return request.getGroupId();
    }

    @Override
    public String toString() {
        return request.toString();
    }
}
