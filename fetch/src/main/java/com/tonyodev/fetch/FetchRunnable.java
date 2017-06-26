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
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.tonyodev.fetch.exception.DownloadInterruptedException;
import com.tonyodev.fetch.request.Header;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
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

    private static final String ACTION_DONE = "com.tonyodev.fetch.action_done";
    private static final String EXTRA_ID = "com.tonyodev.fetch.extra_id";

    private final long id;
    private final String url;
    private final String filePath;
    private final List<Header> headers;
    private final boolean loggingEnabled;
    private final long onUpdateInterval;

    private final Context context;
    private final LocalBroadcastManager broadcastManager;
    private final DatabaseHelper databaseHelper;

    private volatile boolean interrupted = false;

    private HttpURLConnection httpURLConnection;
    private BufferedInputStream input;
    private RandomAccessFile output;

    private int progress;
    private long downloadedBytes;
    private long fileSize;

    @NonNull
    static IntentFilter getDoneFilter() {
        return new IntentFilter(ACTION_DONE);
    }

    FetchRunnable(@NonNull Context context, long id,@NonNull String url,@NonNull String filePath,
                  @NonNull List<Header> headers,long fileSize,boolean loggingEnabled,
                  long onUpdateInterval) {

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
        this.loggingEnabled = loggingEnabled;
        this.onUpdateInterval = onUpdateInterval;
        this.databaseHelper.setLoggingEnabled(loggingEnabled);
    }

    @Override
    public void run() {

        try {

            setHttpConnectionPrefs();
            Utils.createFileOrThrow(filePath);

            downloadedBytes = Utils.getFileSize(filePath);
            progress = Utils.getProgress(downloadedBytes,fileSize);
            databaseHelper.updateFileBytes(id,downloadedBytes,fileSize);

            httpURLConnection.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");

            if (isInterrupted()) {
                throw new DownloadInterruptedException("DIE",ErrorUtils.DOWNLOAD_INTERRUPTED);
            }

            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();

            if (isResponseOk(responseCode)) {

                if (isInterrupted()) {
                    throw new DownloadInterruptedException("DIE",ErrorUtils.DOWNLOAD_INTERRUPTED);
                }

                if(fileSize < 1) {
                    setContentLength();
                    databaseHelper.updateFileBytes(id,downloadedBytes,fileSize);
                    progress = Utils.getProgress(downloadedBytes,fileSize);
                }

                output = new RandomAccessFile(filePath,"rw");
                if(responseCode == HttpURLConnection.HTTP_PARTIAL) {
                    output.seek(downloadedBytes);
                }else {
                    output.seek(0);
                }

                input = new BufferedInputStream(httpURLConnection.getInputStream());
                writeToFileAndPost();

                databaseHelper.updateFileBytes(id,downloadedBytes,fileSize);

                if (isInterrupted()) {
                    throw new DownloadInterruptedException("DIE",ErrorUtils.DOWNLOAD_INTERRUPTED);

                }else if(downloadedBytes >= fileSize && !isInterrupted()) {

                    if(fileSize < 1) {
                        fileSize = Utils.getFileSize(filePath);
                        databaseHelper.updateFileBytes(id,downloadedBytes,fileSize);
                        progress = Utils.getProgress(downloadedBytes,fileSize);
                    }else {
                        progress = Utils.getProgress(downloadedBytes,fileSize);
                    }

                    boolean updated = databaseHelper.updateStatus(id,FetchConst.STATUS_DONE,
                            FetchConst.DEFAULT_EMPTY_VALUE);

                    if(updated){

                        Utils.sendEventUpdate(broadcastManager,id,FetchConst.STATUS_DONE,
                                progress,downloadedBytes,fileSize,FetchConst.DEFAULT_EMPTY_VALUE);
                    }
                }

            } else {
                throw new IllegalStateException("SSRV:" + responseCode);
            }

        }catch (Exception exception) {

            if(loggingEnabled) {
                exception.printStackTrace();
            }

            int error = ErrorUtils.getCode(exception.getMessage());

            if(canRetry(error)) {

                boolean updated = databaseHelper.updateStatus(id,FetchConst.STATUS_QUEUED,
                        FetchConst.DEFAULT_EMPTY_VALUE);

                if(updated) {
                    Utils.sendEventUpdate(broadcastManager,id,FetchConst.STATUS_QUEUED,
                            progress,downloadedBytes,fileSize,FetchConst.DEFAULT_EMPTY_VALUE);
                }

            } else {

                boolean updated = databaseHelper.updateStatus(id,FetchConst.STATUS_ERROR,error);

                if(updated) {
                    Utils.sendEventUpdate(broadcastManager,id,FetchConst.STATUS_ERROR,progress,
                            downloadedBytes,fileSize,error);
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
        httpURLConnection.setReadTimeout(20_000);
        httpURLConnection.setConnectTimeout(15_000);
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
        try {
            fileSize = downloadedBytes + Long.valueOf(httpURLConnection.getHeaderField("Content-Length"));
        } catch (Exception e) {
            fileSize = -1;
        }
    }

    private void writeToFileAndPost() throws IOException {

        byte[] buffer = new byte[1024];
        int read;
        long startTime;
        long stopTime;

        startTime = System.nanoTime();

        while((read = input.read(buffer,0,1024)) != -1 && !isInterrupted()) {
            output.write(buffer,0,read);
            downloadedBytes += read;

            stopTime = System.nanoTime();

            if (Utils.hasIntervalElapsed(startTime,stopTime,onUpdateInterval) && !isInterrupted()) {

                progress = Utils.getProgress(downloadedBytes,fileSize);

                Utils.sendEventUpdate(broadcastManager,id, FetchConst.STATUS_DOWNLOADING,
                        progress,downloadedBytes,fileSize,FetchConst.DEFAULT_EMPTY_VALUE);

                databaseHelper.updateFileBytes(id,downloadedBytes,fileSize);

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
                case ErrorUtils.DOWNLOAD_INTERRUPTED:
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

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        try {
            if (output != null) {
                output.close();
            }
        }catch (IOException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
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

    synchronized void interrupt() {
        this.interrupted = true;
    }

    synchronized long getId() {
        return id;
    }

    static long getIdFromIntent(Intent intent) {

        if(intent == null) {
            return -1;
        }

        return intent.getLongExtra(EXTRA_ID,-1);
    }
}