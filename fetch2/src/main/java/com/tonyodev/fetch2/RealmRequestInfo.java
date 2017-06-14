package com.tonyodev.fetch2;

import android.support.v4.util.ArrayMap;

import java.util.Map;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmRequestInfo extends RealmObject {

    @PrimaryKey
    private long id;
    private String url;
    private String absoluteFilePath;
    private int status;
    private long downloadedBytes;
    private long totalBytes;
    private int error;
    private RealmList<RealmHeader> headers;

    public RealmRequestInfo() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAbsoluteFilePath() {
        return absoluteFilePath;
    }

    public void setAbsoluteFilePath(String absoluteFilePath) {
        this.absoluteFilePath = absoluteFilePath;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public RealmList<RealmHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(RealmList<RealmHeader> headers) {
        this.headers = headers;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    RequestData toRequestData() {

        Map<String,String> headerMap = new ArrayMap<>();

        for (RealmHeader header : headers) {
            headerMap.put(header.getKey(),header.getValue());
        }

        return new RequestData(url,absoluteFilePath,status,error,downloadedBytes,totalBytes,headerMap);
    }

    static RealmRequestInfo newInstance(long id, String url, String absoluteFilePath) {

        RealmRequestInfo realmRequestInfo = new RealmRequestInfo();
        realmRequestInfo.setId(id);
        realmRequestInfo.setUrl(url);
        realmRequestInfo.setAbsoluteFilePath(absoluteFilePath);
        realmRequestInfo.setStatus(Status.QUEUED.getValue());
        realmRequestInfo.setTotalBytes(0L);
        realmRequestInfo.setDownloadedBytes(0L);
        realmRequestInfo.setError(Error.NONE.getValue());
        realmRequestInfo.setHeaders(new RealmList<RealmHeader>());

        return realmRequestInfo;
    }
}
