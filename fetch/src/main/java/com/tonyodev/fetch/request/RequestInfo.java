/*
 * Copyright (C) 2017 Tonyo Francis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tonyodev.fetch.request;


import android.support.annotation.NonNull;

import java.util.List;

/**
 * RequestInfo is an immutable class that holds
 * a snapshot of a download request's information.
 *
 * Do not hold onto instances of this class for long. They may become invalid
 * if Fetch or the FetchService updates the status or progress of the download
 * request. Use them to quickly update your models or user interface.
 *
 * @author Tonyo Francis
 */

public final class RequestInfo {

    private final long id;
    private final int status;
    private final String url;
    private final String filePath;
    private final int progress;
    private final long downloadedBytes;
    private final long fileSize;
    private final int error;
    private final List<Header> headers;
    private final int priority;

    public RequestInfo(long id, int status, @NonNull String url, @NonNull String filePath, int progress,
                       long downloadedBytes, long fileSize, int error, @NonNull List<Header> headers, int priority) {

        if(url == null) {
            throw new NullPointerException("Url cannot be null");
        }

        if(filePath == null) {
            throw new NullPointerException("FilePath cannot be null");
        }

        if(headers == null) {
            throw new NullPointerException("Headers cannot be null");
        }

        this.id = id;
        this.status = status;
        this.url = url;
        this.filePath = filePath;
        this.progress = progress;
        this.downloadedBytes = downloadedBytes;
        this.fileSize = fileSize;
        this.error = error;
        this.headers = headers;
        this.priority = priority;
    }

    /**
     * @return the unique ID token of the download request.
     * */
    public long getId() {
        return id;
    }

    /**
     * @return the status of the download request.
     * */
    public int getStatus() {
        return status;
    }

    /**
     * @return the download url of the download request.
     * */
    @NonNull
    public String getUrl() {
        return url;
    }

    /**
     * @return the local absolute path including file name where the downloaded file is stored on the device
     * or SD Card. eg: /storage/videos/video.mp4
     * */
    @NonNull
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return the current download progress/percentage of the download request.
     * */
    public int getProgress() {
        return progress;
    }

    /**
     * @return the downloaded bytes of the download request.
     * */
    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    /**
     * @return the file size of the file to be downloaded.
     * */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * @return the error value of a download request if its status is set to STATUS_ERROR.
     * */

    public int getError() {
        return error;
    }

    /**
     * @return the headers of a download request.
     * */
    @NonNull
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     * @return the priority of a download request.
     * */
    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {

        StringBuilder headerBuilder = new StringBuilder();

        for (Header header : headers) {
            headerBuilder.append(header.toString())
                    .append(",");
        }

        if(headers.size() > 0) {
            headerBuilder.deleteCharAt(headerBuilder.length()-1);
        }

        return "{id:" + id + ",status:" + status + ",url:" + url
                + ",filePath:" + filePath + ",progress:" + progress
                + ",fileSize:" + fileSize + ",error:" + error
                + ",headers:{" + headerBuilder.toString() + "}"
                + ",priority:" + priority + "}";
    }
}