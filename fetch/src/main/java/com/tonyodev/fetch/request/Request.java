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

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.tonyodev.fetch.Fetch;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains all the information necessary to request a new download
 * with Fetch.
 *
 * @author Tonyo Francis
 */

public final class Request {

    private final String url;
    private final String filePath;
    private final List<Header> headers = new ArrayList<>();
    private int priority = Fetch.PRIORITY_NORMAL;

    /**
     * This class contains all the information necessary to request a new download with Fetch.
     *
     * @param url The download url where the file can be downloaded from. This parameter cannot
     *            be null.
     * @param filePath the absolute local file path where the downloaded file will be stored. This
     *                 parameter cannot be null.
     *
     * @throws NullPointerException if the url or filePath parameters are null.
     * @throws IllegalArgumentException if the url does not have an http or https scheme.
     * */
    public Request(@NonNull String url,@NonNull String filePath) {

        if(url == null) {
            throw new NullPointerException("Url cannot be null");
        }

        if(filePath == null) {
            throw new NullPointerException("File  Path cannot be null");
        }

        String scheme = Uri.parse(url).getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + url);
        }

        this.url = url;
        this.filePath = filePath;
    }

    /**
     * An HTTP header to be included with the download request.
     *
     * @param header HTTP header name.
     * @param value Header value.
     *
     * @return the same instance of Request.
     *
     * @throws NullPointerException if the HTTP header name is null.
     * @throws IllegalArgumentException if the header contains a ':'.
     * */
    @NonNull
    public Request addHeader(@NonNull String header,@Nullable String value) {

        headers.add(new Header(header,value));
        return this;
    }

    /**
     * Sets the download priority of the request.
     *
     * @param priority priority of the download. PRIORITY_HIGH, PRIORITY_NORMAL
     *
     * @return the same instance of Request.
     * */
    @NonNull
    public Request setPriority(int priority) {

        this.priority = Fetch.PRIORITY_NORMAL;

        if(priority == Fetch.PRIORITY_HIGH) {
            this.priority = Fetch.PRIORITY_HIGH;
        }

        return this;
    }

    /**
     *
     * @return  the download url where the file can be downloaded from.
     * */
    @NonNull
    public String getUrl() {
        return url;
    }

    /**
     * @return the absolute local file path where the downloaded file will be stored.
     * */
    @NonNull
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return list of headers attached to this request.
     * */
    @NonNull
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     * @return the request priority.
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

        return "{url:" + url + " ,filePath:" + filePath + ",headers:{"
                + headerBuilder.toString() + "}"
                + ",priority:" + priority + "}";
    }
}