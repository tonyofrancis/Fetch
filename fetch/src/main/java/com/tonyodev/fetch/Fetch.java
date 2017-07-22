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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.tonyodev.fetch.callback.FetchCall;
import com.tonyodev.fetch.callback.FetchTask;
import com.tonyodev.fetch.exception.EnqueueException;
import com.tonyodev.fetch.exception.InvalidStatusException;
import com.tonyodev.fetch.exception.NotUsableException;
import com.tonyodev.fetch.listener.FetchListener;
import com.tonyodev.fetch.request.Request;
import com.tonyodev.fetch.request.RequestInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Fetch is a download manager for the FetchService.
 * Instances of this class listen for status and progress updates from
 * the FetchService and notifies attached FetchListeners of these changes.
 *
 * Instances of this class are obtained by calling the Fetch.newInstance(Context) method.
 *
 * @author Tonyo Francis
 */
public final class Fetch implements FetchConst {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ConcurrentMap<Request,FetchCallRunnable> callsMap = new ConcurrentHashMap<>();

    private final Context context;
    private final LocalBroadcastManager broadcastManager;
    private final List<FetchListener> listeners = new ArrayList<>();
    private final DatabaseHelper dbHelper;
    private volatile boolean isReleased = false;

    private Fetch(Context context) {

        this.context = context.getApplicationContext();

        this.broadcastManager = LocalBroadcastManager.getInstance(this.context);
        this.dbHelper = DatabaseHelper.getInstance(this.context);
        this.dbHelper.setLoggingEnabled(isLoggingEnabled());

        broadcastManager.registerReceiver(updateReceiver,
                FetchService.getEventUpdateFilter());

        this.context.registerReceiver(networkReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        startService(this.context);
    }

    /**
     * Starts the FetchService and begins processing/downloading any
     * Fetch.STATUS_QUEUED Requests in the background.
     *
     * <p>Call this method if you need queued downloads to start
     * on application launch, or in a JobService without getting
     * an instance of Fetch.
     *
     * @param context context used to start the service.
     *
     * @throws NullPointerException if the passed in context is null.
     * */
    public static void startService(@NonNull Context context) {

        FetchService.processPendingRequests(context);
    }

    /**
     * @deprecated <p>See {@link Fetch#newInstance(Context)}
     * */
    public static Fetch getInstance(@NonNull Context context) {

        return newInstance(context);
    }

    /**
     * Gets a new instance of Fetch.
     *
     * @param context Context
     *
     * @return a new instance of Fetch
     *
     * @throws NullPointerException if context is null
     * */
    public static Fetch newInstance(@NonNull Context context) {

        if(context == null) {
            throw new NullPointerException("Context cannot be null");
        }

        return new Fetch(context);
    }

    /**
     * Runs a GET request in the background and returns the response as a String.
     * Experimental Feature. The implementation of Fetch.Call() may change in
     * the future.
     *
     * @param request a download request. Cannot be null.
     * @param fetchCall Callback used to return the GET response/data back to the caller.
     *                  Cannot be null.
     *
     * @throws NullPointerException if request is null.
     * @throws NullPointerException if the callback is null.
     * */
    public static void call(@NonNull Request request,@NonNull FetchCall<String> fetchCall) {

        if(request == null) {
            throw new NullPointerException("Request cannot be null");
        }

        if(fetchCall == null) {
            throw new NullPointerException("FetchCall cannot be null");
        }

        if(callsMap.containsKey(request)) {
            return;
        }

        FetchCallRunnable callRunnable = new FetchCallRunnable(request,fetchCall,callsCallback);

        callsMap.put(request,callRunnable);

        new Thread(callRunnable).start();
    }

    private static final FetchCallRunnable.Callback callsCallback = new FetchCallRunnable.Callback() {
        @Override
        public void onDone(Request request) {
            callsMap.remove(request);
        }
    };

    /**
     * Cancels a currently running FetchCall.
     *
     * @param request Request used to start the FetchCall.
     * */
    public static void cancelCall(@NonNull Request request) {

        if(request == null) {
            return;
        }

        if(callsMap.containsKey(request)) {

            FetchCallRunnable fetchCallRunnable = callsMap.get(request);

            if(fetchCallRunnable != null) {
                fetchCallRunnable.interrupt();
            }
        }
    }

    /**
     * Performs cleanup on this Fetch. Call this method only after you
     * are completely done with this instance.
     *
     * <p>Method calls on this Fetch instance will throw a NotUsableException,
     * after this method is called. If needed, get a new instance of Fetch by calling
     * Fetch.newInstance().
     *
     * */
    public void release() {

        if(!isReleased()) {

            setReleased(true);
            listeners.clear();
            broadcastManager.unregisterReceiver(updateReceiver);
            context.unregisterReceiver(networkReceiver);
        }
    }

    /**
     * Adds a FetchListener that will be notified of a download request's status and progress
     * by Fetch.
     *
     * @param fetchListener a FetchListener instance that will be notified of status and progress
     *                      updates.
     *
     * @throws NullPointerException if the passed in FetchListener is null.
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void addFetchListener(@NonNull FetchListener fetchListener) {

        Utils.throwIfNotUsable(this);

        if(fetchListener == null) {
            throw new NullPointerException("fetchListener cannot be null");
        }

        if(listeners.contains(fetchListener)) {
            return;
        }

        listeners.add(fetchListener);
    }

    /**
     * Removes a FetchListener from Fetch. The removed listener will no longer be notified
     * of status and progress updates.
     *
     * @param fetchListener the FetchListener to delete.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void removeFetchListener(@NonNull FetchListener fetchListener) {

        Utils.throwIfNotUsable(this);

        if(fetchListener == null) {
            return;
        }

        listeners.remove(fetchListener);
    }

    /**
     * Removes all FetchListeners from Fetch. The removed listeners will no longer be notified
     * of status and progress updates.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void removeFetchListeners() {

        Utils.throwIfNotUsable(this);
        listeners.clear();
    }

    /**
     * Enqueues the new download request for downloading.
     *
     * @param request a download request.
     *
     * @return a unique ID used by Fetch and the FetchService to identify a download
     *         request. If the request could not be enqueued -1 is returned.
     *
     * @throws NullPointerException if the passed in request is null.
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public long enqueue(@NonNull Request request) {

        Utils.throwIfNotUsable(this);

        if(request == null) {
            throw new NullPointerException("Request cannot be null");
        }

        long id = Utils.generateRequestId();

        try {

            String url = request.getUrl();
            String filePath = request.getFilePath();
            int priority = request.getPriority();
            String headers = Utils.headerListToString(request.getHeaders(),isLoggingEnabled());
            long fileSize = 0L;
            long downloadedBytes = 0L;

            File file = Utils.getFile(filePath);

            if (file.exists()) {
                downloadedBytes = file.length();
            }

            boolean enqueued = dbHelper.insert(id,url,filePath, Fetch.STATUS_QUEUED,headers,downloadedBytes,
                    fileSize,priority, DEFAULT_EMPTY_VALUE);

            if(!enqueued) {
                throw new EnqueueException("could not insert request",ERROR_ENQUEUE_ERROR);
            }

            startService(context);

        }catch (EnqueueException e) {

            if(isLoggingEnabled()) {
                e.printStackTrace();
            }

            id = DEFAULT_EMPTY_VALUE;
        }

        return id;
    }

    /**
     * Enqueues a list of new download requests for downloading.
     *
     * @param requests a list of download requests.
     *
     * @return a list with unique IDs used by Fetch and the FetchService to identify
     *         the download requests. If a request could not be enqueued an ID of -1
     *         is returned for that request.
     *
     * @throws NullPointerException if the passed in list is null.
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    @NonNull
    public List<Long> enqueue(@NonNull List<Request> requests) {

        Utils.throwIfNotUsable(this);

        if(requests == null) {
            throw new NullPointerException("Request list cannot be null");
        }

        if (requests.size() < 1) {
            return new ArrayList<>(0);
        }

        List<Long> ids = new ArrayList<>(requests.size());
        List<String> statements = new ArrayList<>();

        long id;
        String url;
        String filePath;
        String headers;
        int status;
        int priority;
        long downloadedBytes;
        long fileSize;
        int error;

        try {

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dbHelper.getInsertStatementOpen());

            for (Request request : requests) {

                id = DEFAULT_EMPTY_VALUE;

                if(request != null) {

                    id = Utils.generateRequestId();
                    url = request.getUrl();
                    filePath = request.getFilePath();
                    headers = Utils.headerListToString(request.getHeaders(),isLoggingEnabled());
                    status = Fetch.STATUS_QUEUED;
                    priority = request.getPriority();
                    downloadedBytes = 0L;
                    fileSize = 0L;

                    File file = Utils.getFile(filePath);

                    if (file.exists()) {
                        downloadedBytes = file.length();
                    }

                    error = DEFAULT_EMPTY_VALUE;

                    String statement = dbHelper.getRowInsertStatement(id,url,filePath,status,headers, downloadedBytes,fileSize,priority,error);
                    stringBuilder.append(statement)
                                 .append(", ");
                }

                ids.add(id);
            }

            stringBuilder.delete(stringBuilder.length()-2,stringBuilder.length())
                         .append(dbHelper.getInsertStatementClose());
            boolean inserted = dbHelper.insert(stringBuilder.toString());

            if(!inserted) {
                throw new EnqueueException("could not insert requests",ERROR_ENQUEUE_ERROR);
            }

            startService(context);
        }catch (EnqueueException e) {

            if(isLoggingEnabled()) {
                e.printStackTrace();
            }

            ids.clear();
            for (int i = 0; i < requests.size(); i++) {
                ids.add(-1L);
            }
        }

        return ids;
    }

    /**
     * Removes a download request completely from the FetchService. If the request is currently
     * downloading, the download will be halted. Calling this method will also delete the
     * partial or fully downloaded file on the device or SD Card.
     *
     * @param id a unique ID used by Fetch and the FetchService to identify a download
     *           request.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void remove(long id) {

        Utils.throwIfNotUsable(this);

        Bundle extras = new Bundle();
        extras.putInt(FetchService.ACTION_TYPE, FetchService.ACTION_REMOVE);
        extras.putLong(FetchService.EXTRA_ID,id);

        FetchService.sendToService(context,extras);
    }

    /**
     * Removes all download requests completely from the FetchService. If a request is currently
     * downloading, the download will be halted. Calling this method will also delete the
     * partial or fully downloaded files on the device or SD Card.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void removeAll() {

        Utils.throwIfNotUsable(this);

        Bundle extras = new Bundle();
        extras.putInt(FetchService.ACTION_TYPE, FetchService.ACTION_REMOVE_ALL);

        FetchService.sendToService(context,extras);
    }

    /**
     * Remove a request from Fetch. The file is not deleted.
     * @param id request Id
     * */
    public void removeRequest(long id) {

        Utils.throwIfNotUsable(this);

        Bundle extras = new Bundle();
        extras.putInt(FetchService.ACTION_TYPE, FetchService.ACTION_REMOVE_REQUEST);
        extras.putLong(FetchService.EXTRA_ID,id);

        FetchService.sendToService(context,extras);

    }

    /**
     * Remove a requests from Fetch. The files are not deleted.
     * */
    public void removeRequests() {

        Utils.throwIfNotUsable(this);


        Bundle extras = new Bundle();
        extras.putInt(FetchService.ACTION_TYPE, FetchService.ACTION_REMOVE_REQUEST_ALL);

        FetchService.sendToService(context,extras);
    }

    /**
     * Sets the status of a download request to STATUS_PAUSED.
     *
     * <p>The Fetch.STATUS_PAUSED status will only be set for the download request if its current status
     * is Fetch.STATUS_QUEUED or Fetch.STATUS_DOWNLOADING.
     *
     * @param id a unique ID used by Fetch and the FetchService to identify a download
     *           request.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void pause(long id) {

        Utils.throwIfNotUsable(this);

        Bundle extras = new Bundle();
        extras.putInt(FetchService.ACTION_TYPE, FetchService.ACTION_PAUSE);
        extras.putLong(FetchService.EXTRA_ID,id);

        FetchService.sendToService(context,extras);
    }

    /**
     * Sets the status of a paused download request to Fetch.STATUS_QUEUED. The FetchService
     * will queue the request, and resume the download.
     *
     * @param id a unique ID used by Fetch and the FetchService to identify a download
     *           request.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void resume(long id) {

        Utils.throwIfNotUsable(this);

        Bundle extras = new Bundle();
        extras.putInt(FetchService.ACTION_TYPE, FetchService.ACTION_RESUME);
        extras.putLong(FetchService.EXTRA_ID,id);

        FetchService.sendToService(context,extras);
    }

    /**
     * Sets the allowed network connection type the FetchService can use to download requests.
     *
     * <p>This method only accepts two values: Fetch.NETWORK_WIFI or Fetch.NETWORK_ALL. The default is
     * Fetch.NETWORK_ALL.
     *
     * @param networkType allowed network type
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void setAllowedNetwork(int networkType) {

        Utils.throwIfNotUsable(this);
        new Settings(context).setAllowedNetwork(networkType).apply();
    }

    /**
     * Sets the download priority of a download request
     *
     * @param id a unique ID used by Fetch and the FetchService to identify a download
     *           request.
     *
     * @param priority download priority. Fetch.PRIORITY_HIGH or Fetch.PRIORITY_NORMAL
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void setPriority(long id,int priority) {

        Utils.throwIfNotUsable(this);

        int priorityType = Fetch.PRIORITY_NORMAL;

        if(priority == Fetch.PRIORITY_HIGH) {
            priorityType = Fetch.PRIORITY_HIGH;
        }

        Bundle extras = new Bundle();
        extras.putInt(FetchService.ACTION_TYPE, FetchService.ACTION_PRIORITY);
        extras.putLong(FetchService.EXTRA_ID,id);
        extras.putInt(FetchService.EXTRA_PRIORITY,priorityType);

        FetchService.sendToService(context,extras);
    }

    /**
     * Retries a failed download request.
     *
     * @param id a unique ID used by Fetch and the FetchService to identify a download
     *           request.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void retry(long id) {

        Utils.throwIfNotUsable(this);

        Bundle extras = new Bundle();
        extras.putInt(FetchService.ACTION_TYPE, FetchService.ACTION_RETRY);
        extras.putLong(FetchService.EXTRA_ID,id);

        FetchService.sendToService(context,extras);
    }

    /**
     * Query the FetchService database for a download request.
     *
     * @param id a unique ID used by Fetch and the FetchService to identify a download
     *           request.
     *
     * @return a RequestInfo object that contains the status and progress of a request.
     *         If the request could not be found null will be returned.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    @Nullable
    public synchronized RequestInfo get(long id) {

        Utils.throwIfNotUsable(this);

        Cursor cursor = dbHelper.get(id);

        return Utils.cursorToRequestInfo(cursor,true,isLoggingEnabled());
    }

    /**
     * Query the FetchService database for all download requests.
     *
     * @return a List of RequestInfo object that contains the status and progress of a request.
     *         If no requests are found, an empty list will be returned.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    @NonNull
    public synchronized List<RequestInfo> get() {

        Utils.throwIfNotUsable(this);

        Cursor cursor = dbHelper.get();

        return Utils.cursorToRequestInfoList(cursor,true,isLoggingEnabled());
    }

    /**
     * Query the FetchService database for download requests that matches a list of ids.
     *
     * @param ids IDs of requests
     *
     * @return a List of RequestInfo object that contains the status and progress of a request.
     *         If no requests are found, an empty list will be returned.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    @NonNull
    public synchronized List<RequestInfo> get(long... ids) {

        Utils.throwIfNotUsable(this);

        if(ids == null) {
            return new ArrayList<>();
        }

        Cursor cursor = dbHelper.get(ids);

        return Utils.cursorToRequestInfoList(cursor,true,isLoggingEnabled());
    }

    /**
     * Query the FetchService database for all download requests with the passed in status.
     *
     * @param status eg. Fetch.STATUS_DONE, Fetch.STATUS_QUEUED
     *
     * @return a List of RequestInfo object that contains the status and progress of a request.
     *         If no requests are found, an empty list will be returned.
     *
     * @throws InvalidStatusException if the passed in status is not a valid status.
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    @NonNull
    public synchronized List<RequestInfo> getByStatus(int status) {

        Utils.throwIfNotUsable(this);
        Utils.throwIfInvalidStatus(status);

        Cursor cursor = dbHelper.getByStatus(status);

        return Utils.cursorToRequestInfoList(cursor,true,isLoggingEnabled());
    }

    /**
     * Query the FetchService database for a download request.
     *
     * @param request the request to check. This parameter cannot be null.
     *
     * @throws NullPointerException if the passed in request is null.
     * @throws NotUsableException if the release method has been called on Fetch.
     *
     * @return a RequestInfo object that contains the status and progress of a request.
     *         If the request could not be found null will be returned.
     * */
    @Nullable
    public synchronized RequestInfo get(@NonNull Request request) {

        Utils.throwIfNotUsable(this);

        if(request == null) {
            throw new NullPointerException("Request cannot be null.");
        }

        Cursor cursor = dbHelper.getByUrlAndFilePath(request.getUrl(),request.getFilePath());

        return Utils.cursorToRequestInfo(cursor,true,isLoggingEnabled());
    }

    /**
     * Gets a downloaded file for a download request that
     * has been successfully downloaded.
     *
     * @param id a unique ID used by Fetch and the FetchService to identify a download
     *           request.
     *
     * @return a downloaded file or null if the file has not been successfully downloaded.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    @Nullable
    public synchronized File getDownloadedFile(long id) {

        Utils.throwIfNotUsable(this);

        Cursor cursor = dbHelper.get(id);
        RequestInfo requestInfo = Utils.cursorToRequestInfo(cursor,true,isLoggingEnabled());

        if(requestInfo == null || requestInfo.getStatus() != STATUS_DONE) {
            return null;
        } else {

            File file = Utils.getFile(requestInfo.getFilePath());

            if(file.exists()) {
                return file;
            } else {
                return null;
            }
        }
    }

    /**
     * Query the FetchService database for a download request's file path.
     * The FilePath is the absolute local file path including file name where the downloaded
     * file is stored. eg: /storage/videos/video.mp4
     *
     * @param id a unique ID used by Fetch and the FetchService to identify a download
     *           request.
     *
     * @return the absolute file path of a request download or null if the request
     *          does not exist.
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    @Nullable
    public synchronized String getFilePath(long id) {

        Utils.throwIfNotUsable(this);

        Cursor cursor = dbHelper.get(id);
        RequestInfo requestInfo = Utils.cursorToRequestInfo(cursor,true,isLoggingEnabled());

        if(requestInfo == null) {
            return null;
        }else {
            return requestInfo.getFilePath();
        }
    }

    /**
     * Adds a file to Fetch and the FetchService for management.
     *
     * @param filePath the absolute path of the file that will be managed by the FetchService.
     *
     * @return a unique ID used by Fetch and the FetchService to identify a download
     *           request.
     *
     * @throws NullPointerException if the passed in file path is null.
     * @throws NotUsableException if the release method has been called on Fetch.
     *
     * */
    public long addCompletedDownload(@NonNull String filePath) {

        Utils.throwIfNotUsable(this);

        if (filePath == null) {
            throw new NullPointerException("File path cannot be null");
        }

        long id;

        try {

            if (!Utils.fileExist(filePath)) {
                throw new EnqueueException("File does not exist at filePath: " + filePath,
                        ErrorUtils.REQUEST_ALREADY_EXIST);
            }

            id = Utils.generateRequestId();
            File file = Utils.getFile(filePath);
            String url = Uri.fromFile(file).toString();
            String headers = Utils.headerListToString(null,isLoggingEnabled());
            long fileSize = file.length();

            boolean inserted = dbHelper.insert(id, url, filePath, Fetch.STATUS_DONE, headers,
                    fileSize,fileSize, Fetch.PRIORITY_NORMAL, DEFAULT_EMPTY_VALUE);

            if(!inserted) {
                throw new EnqueueException("could not insert request:" + filePath,ERROR_ENQUEUE_ERROR);
            }

        }catch (EnqueueException e) {

            if(isLoggingEnabled()) {
                e.printStackTrace();
            }

            id = DEFAULT_EMPTY_VALUE;
        }

        return id;
    }

    /**
     * Adds a list of files to Fetch and the FetchService for management.
     *
     * @param filePaths a list of absolute paths of files that will be managed by the FetchService.
     *
     * @return a list with unique IDs used by Fetch and the FetchService to identify
     *         the download requests. If a request could not be enqueued an ID of -1
     *         is returned for that request.
     *
     * @throws NullPointerException if the passed in file path is null.
     * @throws NotUsableException if the release method has been called on Fetch.
     *
     * */
    @NonNull
    public List<Long> addCompletedDownloads(@NonNull List<String> filePaths) {

        Utils.throwIfNotUsable(this);

        if(filePaths == null) {
            throw new NullPointerException("Request list cannot be null");
        }

        if (filePaths.size() < 1) {
            return new ArrayList<>(0);
        }

        List<Long> ids = new ArrayList<>(filePaths.size());
        List<String> statements = new ArrayList<>();

        long id;
        String url;
        String filePath;
        String headers;
        int status;
        int priority;
        long downloadedBytes;
        long fileSize;
        int error;

        try {

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dbHelper.getInsertStatementOpen());
            for (String path : filePaths) {

                id = DEFAULT_EMPTY_VALUE;

                if(path != null) {

                    File file = Utils.getFile(path);

                    if (!file.exists()) {
                        break;
                    }

                    id = Utils.generateRequestId();
                    url = Uri.fromFile(file).toString();
                    filePath = path;
                    headers = Utils.headerListToString(null,isLoggingEnabled());
                    status = Fetch.STATUS_DONE;
                    priority = Fetch.PRIORITY_NORMAL;
                    downloadedBytes = file.length();
                    fileSize = downloadedBytes;
                    error = DEFAULT_EMPTY_VALUE;

                    String statement = dbHelper.getRowInsertStatement(id,url,filePath,status,headers, downloadedBytes,fileSize,priority,error);
                    stringBuilder.append(statement)
                            .append(",");
                }

                ids.add(id);
            }

            stringBuilder.delete(stringBuilder.length()-2,stringBuilder.length())
                         .append(dbHelper.getInsertStatementClose());
            boolean inserted = dbHelper.insert(stringBuilder.toString());

            if(!inserted) {
                throw new EnqueueException("could not insert requests",ERROR_ENQUEUE_ERROR);
            }

        }catch (EnqueueException e) {

            if(isLoggingEnabled()) {
                e.printStackTrace();
            }

            ids.clear();
            for (int i = 0; i < filePaths.size(); i++) {
                ids.add(-1L);
            }
        }

        return ids;
    }

    /**
     * Runs a Task on a background thread. Use this method to run short tasks
     * off the main thread.
     *
     * @param fetchTask a FetchTask that will be executed on a background thread.
     *
     * @throws NullPointerException if the passed in FetchTask is null.
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void runOnBackgroundThread(@NonNull final FetchTask fetchTask) {

        Utils.throwIfNotUsable(this);
        Utils.throwIfFetchTaskNull(fetchTask);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Fetch fetch = Fetch.newInstance(context);
                fetchTask.onProcess(fetch);
                fetch.release();
            }
        }).start();
    }

    /**
     * Runs a short Task on the Main Thread. Use this method to update views etc.
     *
     * @param fetchTask a FetchTask that will be executed on the Main Thread.
     *
     * @throws NullPointerException if the passed in FetchTask is null.
     * @throws NotUsableException if the release method has been called on Fetch.
     * */
    public void runOnMainThread(@NonNull final FetchTask fetchTask) {

        Utils.throwIfNotUsable(this);
        Utils.throwIfFetchTaskNull(fetchTask);

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Fetch fetch = Fetch.newInstance(context);
                fetchTask.onProcess(fetch);
                fetch.release();
            }
        });
    }

    /**
     * Checks if a request is already being managed by the Fetch Service.
     *
     * @param request the request to check. This parameter cannot be null.
     *
     * @throws NullPointerException if the passed in request is null.
     * @throws NotUsableException if the release method has been called on Fetch.
     *
     * @return returns true if the request is being managed by the Fetch Service
     *         or false if it is not.
     * */
    public synchronized boolean contains(@NonNull Request request) {

        Utils.throwIfNotUsable(this);

        if(request == null) {
            throw new NullPointerException("Request cannot be null.");
        }

        Cursor cursor = dbHelper.getByUrlAndFilePath(request.getUrl(),request.getFilePath());

        return Utils.containsRequest(cursor,true);
    }

    /**
     * @return returns true if this instance of Fetch is still
     * valid for use.
     * */
    public boolean isValid() {
        return !isReleased();
    }

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {

        private long id;
        private int status;
        private int progress;
        private long downloadedBytes;
        private long fileSize;
        private int error;

        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent == null) {
                return;
            }

            id = intent.getLongExtra(FetchService.EXTRA_ID, DEFAULT_EMPTY_VALUE);
            status = intent.getIntExtra(FetchService.EXTRA_STATUS,DEFAULT_EMPTY_VALUE);
            progress = intent.getIntExtra(FetchService.EXTRA_PROGRESS,DEFAULT_EMPTY_VALUE);
            downloadedBytes = intent.getLongExtra(FetchService.EXTRA_DOWNLOADED_BYTES,DEFAULT_EMPTY_VALUE);
            fileSize = intent.getLongExtra(FetchService.EXTRA_FILE_SIZE,DEFAULT_EMPTY_VALUE);
            error = intent.getIntExtra(FetchService.EXTRA_ERROR,DEFAULT_EMPTY_VALUE);

            for (FetchListener listener : listeners) {
                listener.onUpdate(id,status,progress,downloadedBytes,fileSize,error);
            }
        }
    };

    private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            FetchService.processPendingRequests(context);
        }
    };

    boolean isReleased() {
        return isReleased;
    }

    private void setReleased(boolean released) {
        isReleased = released;
    }

    private boolean isLoggingEnabled() {
        return FetchService.isLoggingEnabled(context);
    }

    /**
     * Enables or Disables console logging
     * for Fetch and the FetchService. Logging
     * is ON by default.
     *
     * @param enabled enable or disable console logging
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     */
    public void enableLogging(boolean enabled) {

        Utils.throwIfNotUsable(this);
        new Settings(context).enableLogging(enabled).apply();
    }

    /**
     * Sets the allowed concurrent downloads. Default 1.
     *
     * @param limit concurrent downloads limit
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     *
     * */
    public void setConcurrentDownloadsLimit(int limit) {

        Utils.throwIfNotUsable(this);
        new Settings(context).setConcurrentDownloadsLimit(limit).apply();
    }

    /**
     * Sets the desired interval for the desired "onUpdate" calls during file download.
     *
     * @param intervalMs the milliseconds interval
     *
     * @throws NotUsableException if the release method has been called on Fetch.
     */
    public void setOnUpdateInterval(long intervalMs) {

        Utils.throwIfNotUsable(this);
        new Settings(context).setOnUpdateInterval(intervalMs).apply();
    }

    /**
     * Updates the url for an existing request
     *
     * @param id request id
     * @param url new url
     *
     * @throws NotUsableException if the release method has been called on Fetch
     * */
    public void updateUrlForRequest(long id,@Nullable String url) {

        Utils.throwIfNotUsable(this);

        if(url == null) {
            throw new NullPointerException("Url cannot be null");
        }

        Utils.throwIfInvalidUrl(url);

        Bundle extras = new Bundle();
        extras.putInt(FetchService.ACTION_TYPE, FetchService.ACTION_UPDATE_REQUEST_URL);
        extras.putLong(FetchService.EXTRA_ID,id);
        extras.putString(FetchService.EXTRA_URL,url);

        FetchService.sendToService(context,extras);
    }

    /**
     * The Settings class is used to apply
     * settings to Fetch and the FetchService.
     * */
    public static class Settings {

        private final Context context;
        private final List<Bundle> settings = new ArrayList<>();

        public Settings(@NonNull Context context) {

            if(context == null) {
                throw new NullPointerException("Context cannot be null");
            }
           this.context = context;
        }

        /**
         * Enables or Disables console logging
         * for Fetch and the FetchService. Logging
         * is ON by default.
         *
         * @param enabled enable or disable console logging
         *
         * @return the settings instance
         */
        public Settings enableLogging(boolean enabled) {

            Bundle extras = new Bundle();
            extras.putInt(FetchService.ACTION_TYPE,FetchService.ACTION_LOGGING);
            extras.putBoolean(FetchService.EXTRA_LOGGING_ID,enabled);
            settings.add(extras);

            return this;
        }

        /**
         * Sets the allowed network connection type the FetchService can use to download requests.
         *
         * <p>This method only accepts two values: {@link FetchConst#NETWORK_WIFI} or {@link FetchConst#NETWORK_ALL}.
         * The default is {@link FetchConst#NETWORK_ALL}.
         *
         * @param networkType allowed network type
         *
         * @return the settings instance
         * */
        public Settings setAllowedNetwork(int networkType) {

            int type = NETWORK_ALL;

            if (networkType == NETWORK_WIFI) {
                type = NETWORK_WIFI;
            }

            Bundle extras = new Bundle();
            extras.putInt(FetchService.ACTION_TYPE,FetchService.ACTION_NETWORK);
            extras.putInt(FetchService.EXTRA_NETWORK_ID,type);
            settings.add(extras);

            return this;
        }

        /**
         * Sets the allowed concurrent downloads. Default is 1
         *
         * @param limit concurrent downloads limit
         *
         * @return the settings instance
         * */
        public Settings setConcurrentDownloadsLimit(int limit) {

            Bundle extras = new Bundle();
            extras.putInt(FetchService.ACTION_TYPE,FetchService.ACTION_CONCURRENT_DOWNLOADS_LIMIT);
            extras.putInt(FetchService.EXTRA_CONCURRENT_DOWNLOADS_LIMIT,limit);
            settings.add(extras);

            return this;
        }

        /**
         * Sets the desired interval for the desired "onUpdate" calls during file download.
         *
         * @param intervalMs the milliseconds interval
         *
         * @return the settings instance
         */
        public Settings setOnUpdateInterval(long intervalMs) {

            Bundle extras = new Bundle();
            extras.putInt(FetchService.ACTION_TYPE,FetchService.ACTION_ON_UPDATE_INTERVAL);
            extras.putLong(FetchService.EXTRA_ON_UPDATE_INTERVAL,intervalMs);
            settings.add(extras);

            return this;
        }

        /**
         * Apply the new settings to Fetch and the FetchService
         * */
        public void apply(){

            for (Bundle setting : settings) {
                FetchService.sendToService(context,setting);
            }
        }
    }

}