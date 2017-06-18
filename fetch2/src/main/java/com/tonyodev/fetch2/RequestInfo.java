package com.tonyodev.fetch2;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.v4.util.ArrayMap;

import java.util.Map;



/**
 * Created by tonyofrancis on 6/14/17.
 */

@TypeConverters({DatabaseConverters.class})
@Entity(tableName = "requestInfos",indices = {@Index("url"),@Index("status"),@Index(value = "absoluteFilePath",unique = true)})
public class RequestInfo {
    @PrimaryKey
    private long id;
    private String url;
    private String absoluteFilePath;
    private int status;
    private long downloadedBytes;
    private long totalBytes;
    private int error;
    private Map<String,String> headers;
    private String groupId;

    public RequestInfo() {
    }

    @Ignore
    public RequestInfo(long id, String url, String absoluteFilePath,
                       int status, long downloadedBytes, long totalBytes,
                       int error, Map<String,String> headers,String groupId) {
        this.id = id;
        this.url = url;
        this.absoluteFilePath = absoluteFilePath;
        this.status = status;
        this.downloadedBytes = downloadedBytes;
        this.totalBytes = totalBytes;
        this.error = error;
        this.headers = headers;
        this.groupId = groupId;
    }

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

    public Map<String,String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String,String> headers) {
        this.headers = headers;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Ignore
    RequestData toRequestData() {

        return new RequestData(url,absoluteFilePath,status,error,downloadedBytes,totalBytes,headers,groupId);
    }

    @Ignore
    static RequestInfo newInstance(long id, String url, String absoluteFilePath,String groupId) {

        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setId(id);
        requestInfo.setUrl(url);
        requestInfo.setAbsoluteFilePath(absoluteFilePath);
        requestInfo.setStatus(Status.QUEUED.getValue());
        requestInfo.setTotalBytes(0L);
        requestInfo.setDownloadedBytes(0L);
        requestInfo.setError(Error.NONE.getValue());
        requestInfo.setHeaders(new ArrayMap<String, String>());
        requestInfo.setGroupId(groupId);

        return requestInfo;
    }
}
