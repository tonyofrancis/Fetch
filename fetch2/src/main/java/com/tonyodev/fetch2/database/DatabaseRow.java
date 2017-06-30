package com.tonyodev.fetch2.database;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.v4.util.ArrayMap;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.RequestData;
import com.tonyodev.fetch2.Status;

import java.util.Map;



/**
 * Created by tonyofrancis on 6/14/17.
 */

@android.arch.persistence.room.TypeConverters({TypeConverters.class})
@Entity(tableName = "fetch",indices = {@Index("url"),@Index("status"),@Index(value = "absoluteFilePath",unique = true)})
public class DatabaseRow {
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

    public DatabaseRow() {
    }

    @Ignore
    public DatabaseRow(long id, String url, String absoluteFilePath,
                       int status, long downloadedBytes, long totalBytes,
                       int error, Map<String,String> headers, String groupId) {
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

    public RequestData toRequestData() {
        return new RequestData(url,absoluteFilePath,status,error,downloadedBytes,totalBytes,headers,groupId);
    }

    public static DatabaseRow newInstance(long id, String url, String absoluteFilePath, String groupId) {

        DatabaseRow databaseRow = new DatabaseRow();
        databaseRow.setId(id);
        databaseRow.setUrl(url);
        databaseRow.setAbsoluteFilePath(absoluteFilePath);
        databaseRow.setStatus(Status.QUEUED.getValue());
        databaseRow.setTotalBytes(0L);
        databaseRow.setDownloadedBytes(0L);
        databaseRow.setError(Error.NONE.getValue());
        databaseRow.setHeaders(new ArrayMap<String, String>());
        databaseRow.setGroupId(groupId);

        return databaseRow;
    }
}
