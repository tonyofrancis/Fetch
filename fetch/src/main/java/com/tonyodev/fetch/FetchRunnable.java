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
package com.tonyodev.fetch;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;

import com.tonyodev.fetch.request.Header;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * FetchRunnable assists the FetchService
 * with downloading and reporting a download request status and progress.
 *
 * @author Tonyo Francis
 */
final class FetchRunnable implements Runnable {

    static final String ACTION_DONE = "com.tonyodev.fetch.action_done";
    static final String EXTRA_ID = "com.tonyodev.fetch.extra_id";

    private final long id;
    private final String url;
    private final String filePath;
    private final List<Header> headers;

    private final Context context;
    private final LocalBroadcastManager broadcastManager;
    private final DatabaseHelper databaseHelper;

    private volatile boolean interrupted = false;

    private HttpURLConnection httpURLConnection;
    private InputStream input;
    private FileOutputStream output;

    private int progress = 0;
    private long writtenBytes = 0;
    private long fileSize = -1;

    static IntentFilter getDoneFilter() {
        return new IntentFilter(ACTION_DONE);
    }

    FetchRunnable(Context context, long id, String url, String filePath,
                  List<Header> headers,long fileSize) {

        if(context == null) {
            throw new NullPointerException("Context cannot be null");
        }

        if(url == null) {
            throw new NullPointerException("Url cannot be null");
        }

        if(filePath == null) {
            throw new NullPointerException("FilePath cannot be null");
        }

        if(headers == null) {
            this.headers = new ArrayList<>();
        }else {
            this.headers = headers;
        }

        this.id = id;
        this.url = url;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.context = context.getApplicationContext();
        this.broadcastManager = LocalBroadcastManager.getInstance(this.context);
        this.databaseHelper = DatabaseHelper.getInstance(this.context);
    }

    @Override
    public void run() {

        try {

            setHttpConnectionPrefs();
            Utils.createFileOrThrow(filePath);

            writtenBytes = Utils.getFileSize(filePath);
            progress = Utils.getProgress(writtenBytes,fileSize);
            databaseHelper.updateFileBytes(id,writtenBytes,fileSize);

            httpURLConnection.setRequestProperty("Range", "bytes=" + writtenBytes + "-");

            if (isInterrupted()) {
                throw new InterruptedException("TI");
            }

            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();

            if (isResponseOk(responseCode)) {

                if (isInterrupted()) {
                    throw new InterruptedException("TI");
                }

                setContentLength();
                databaseHelper.updateFileBytes(id,writtenBytes,fileSize);
                progress = Utils.getProgress(writtenBytes,fileSize);
                databaseHelper.updateStatus(id,FetchConst.STATUS_DOWNLOADING,FetchConst.DEFAULT_EMPTY_VALUE);

                input = httpURLConnection.getInputStream();

                if(responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    output = new FileOutputStream(filePath, true);
                }else {
                    output = new FileOutputStream(filePath,false);
                }

                writeToFileAndPost();
                databaseHelper.updateFileBytes(id,writtenBytes,fileSize);

                if(writtenBytes >= fileSize && !isInterrupted()) {

                    if(fileSize == -1) {
                        fileSize = Utils.getFileSize(filePath);
                        databaseHelper.updateFileBytes(id,writtenBytes,fileSize);
                        progress = Utils.getProgress(writtenBytes,fileSize);
                    }

                    boolean updated = databaseHelper.updateStatus(id, FetchConst.STATUS_DONE,
                            FetchConst.DEFAULT_EMPTY_VALUE);

                    if(updated) {

                        Utils.sendEventUpdate(broadcastManager,id, FetchConst.STATUS_DONE,
                                progress,writtenBytes,fileSize,FetchConst.DEFAULT_EMPTY_VALUE);
                    }
                }

            } else {
                throw new IllegalStateException("SSRV:" + responseCode);
            }

        }catch (Exception exception) {

            int error = ErrorUtils.getCode(exception.getMessage());

            if(canRetry(error)) {

                boolean updated = databaseHelper.updateStatus(id, FetchConst.STATUS_QUEUED,
                        FetchConst.DEFAULT_EMPTY_VALUE);

                if(updated) {
                    Utils.sendEventUpdate(broadcastManager,id, FetchConst.STATUS_QUEUED,
                            progress,writtenBytes,fileSize,FetchConst.DEFAULT_EMPTY_VALUE);
                }

            } else {

                boolean updated = databaseHelper.updateStatus(id,FetchConst.STATUS_ERROR,error);

                if(updated) {
                    Utils.sendEventUpdate(broadcastManager,id,FetchConst.STATUS_ERROR,progress,
                            writtenBytes,fileSize,error);
                }
            }

        } finally {
            release();
            broadcastDone();
        }
    }

    private void setHttpConnectionPrefs() throws IOException {

        URL httpUrl = new URL(url);
        httpURLConnection = (HttpURLConnection) httpUrl.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setReadTimeout(15_000);
        httpURLConnection.setConnectTimeout(10_000);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setDefaultUseCaches(false);
        httpURLConnection.setInstanceFollowRedirects(true);
        httpURLConnection.setDoInput(true);

        for (Header header : headers) {
            httpURLConnection.addRequestProperty(header.getHeader(),header.getValue());
        }
    }

    private boolean isResponseOk(int responseCode) {

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_PARTIAL:
            case HttpURLConnection.HTTP_ACCEPTED:
                return true;
            default:
                return false;
        }
    }

    private void setContentLength() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            fileSize = httpURLConnection.getContentLengthLong();
        } else {
            fileSize = httpURLConnection.getContentLength();
        }
    }

    private void writeToFileAndPost() throws IOException {

        byte[] buffer = new byte[1024];
        int read;
        long startTime;
        long stopTime;

        startTime = System.nanoTime();

        while((read = input.read(buffer)) != -1 && !isInterrupted()) {
            output.write(buffer, 0, read);
            writtenBytes += read;

            progress = Utils.getProgress(writtenBytes,fileSize);
            stopTime = System.nanoTime();

            if (Utils.hasTwoSecondsPassed(startTime, stopTime) && !isInterrupted()) {

                Utils.sendEventUpdate(broadcastManager,id, FetchConst.STATUS_DOWNLOADING,
                        progress,writtenBytes,fileSize,FetchConst.DEFAULT_EMPTY_VALUE);

                startTime = System.nanoTime();
            }
        }
    }

    private boolean canRetry(int error) {

        if(!Utils.isNetworkAvailable(context)) {
            return true;
        }else {

            switch (error) {
                case ErrorUtils.CONNECTION_TIMED_OUT:
                case ErrorUtils.THREAD_INTERRUPTED:
                    return true;
                default:
                    return false;
            }
        }
    }

    private void release() {

        try {
            if (input != null) {
                input.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (output != null) {
                output.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        if (httpURLConnection != null) {
            httpURLConnection.disconnect();
        }
    }

    private void broadcastDone() {
        Intent intent = new Intent(ACTION_DONE);
        intent.putExtra(EXTRA_ID,id);
        broadcastManager.sendBroadcast(intent);
    }

    private boolean isInterrupted() {
        return interrupted;
    }

    synchronized void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    long getId() {
        return id;
    }
}