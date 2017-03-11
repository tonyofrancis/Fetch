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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.tonyodev.fetch.exception.EnqueueException;
import com.tonyodev.fetch.request.RequestInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This service allows the queuing, downloading,
 * pausing and resuming of downloads. The FetchService
 * ensures that all downloads are downloaded successfully
 * and reports failed downloads.
 *
 * Status and progress updates for each download request
 * is broadcast. The FetchService can also be queried for request information.
 *
 * @author Tonyo Francis
 */

public final class FetchService extends Service implements FetchConst {

    public static final String EVENT_ACTION_UPDATE = "com.tonyodev.fetch.event_action_update";
    public static final String EVENT_ACTION_ENQUEUED = "com.tonyodev.fetch.event_action_enqueued";
    public static final String EVENT_ACTION_ENQUEUE_FAILED = "com.tonyodev.fetch.event_action_enqueue_failed";
    public static final String EVENT_ACTION_QUERY = "com.tonyodev.fetch.event_action_query";

    public static final String EXTRA_ID = "com.tonyodev.fetch.extra_id";
    public static final String EXTRA_STATUS = "com.tonyodev.fetch.extra_status";
    public static final String EXTRA_PROGRESS = "com.tonyodev.fetch.extra_progress";
    public static final String EXTRA_ERROR = "com.tonyodev.fetch.extra_error";
    public static final String EXTRA_DOWNLOADED_BYTES = "com.tonyodev.fetch.extra_downloaded_bytes";
    public static final String EXTRA_FILE_SIZE = "com.tonyodev.fetch.extra_file_size";
    public static final String EXTRA_URL = "com.tonyodev.fetch.extra_url";
    public static final String EXTRA_FILE_PATH = "com.tonyodev.fetch.extra_file_path";
    public static final String EXTRA_HEADERS = "com.tonyodev.fetch.extra_headers";
    public static final String EXTRA_HEADER_NAME = "com.tonyodev.fetch.extra_header_name";
    public static final String EXTRA_HEADER_VALUE = "com.tonyodev.fetch.extra_header_value";
    public static final String EXTRA_NETWORK_ID = "com.tonyodev.fetch.extra_network_id";
    public static final String EXTRA_QUERY_ID = "com.tonyodev.fetch.extra_query_id";
    public static final String EXTRA_QUERY_RESULT = "com.tonyodev.fetch.extra_query_result";
    public static final String EXTRA_PRIORITY = "com.tonyodev.fetch.extra_priority";
    public static final String EXTRA_QUERY_TYPE = "com.tonyodev.fetch.extra_query_type";

    public static final String ACTION_TYPE = "com.tonyodev.fetch.action_type";

    public static final int ACTION_ENQUEUE = 310;
    public static final int ACTION_PAUSE = 311;
    public static final int ACTION_RESUME = 312;
    public static final int ACTION_REMOVE = 313;
    public static final int ACTION_NETWORK = 314;
    public static final int ACTION_PROCESS_PENDING = 315;
    public static final int ACTION_QUERY = 316;
    public static final int ACTION_PRIORITY = 317;
    public static final int ACTION_RETRY = 318;
    public static final int ACTION_REMOVE_ALL = 319;

    public static final int QUERY_SINGLE = 480;
    public static final int QUERY_ALL = 481;
    public static final int QUERY_BY_STATUS = 482;


    private static final String SHARED_PREFERENCES = "com.tonyodev.fetch.shared_preferences";

    private Context context;
    private DatabaseHelper databaseHelper;
    private LocalBroadcastManager broadcastManager;
    private SharedPreferences sharedPreferences;
    private FetchRunnable fetchRunnable;
    private volatile boolean fetchRunnableQueued = false;
    private volatile boolean removingRequest = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<BroadcastReceiver> registeredReceivers = new ArrayList<>();

    public static void sendToService(@NonNull Context context,@Nullable Bundle extras) {

        if(context == null) {
            throw new NullPointerException("Context cannot be null");
        }

        if(extras == null) {
            extras = new Bundle();
        }

        Intent intent = new Intent(context,FetchService.class);
        intent.putExtras(extras);
        context.startService(intent);
    }

    public static void processPendingRequests(@NonNull Context context) {

        if(context == null) {
            throw new NullPointerException("Context cannot be null");
        }

        Intent intent = new Intent(context,FetchService.class);
        intent.putExtra(FetchService.ACTION_TYPE, FetchService.ACTION_PROCESS_PENDING);
        context.startService(intent);
    }

    public static IntentFilter getEventEnqueuedFilter() {
        return new IntentFilter(EVENT_ACTION_ENQUEUED);
    }

    public static IntentFilter getEventEnqueueFailedFilter() {
        return new IntentFilter(EVENT_ACTION_ENQUEUE_FAILED);
    }

    public static IntentFilter getEventUpdateFilter() {
        return new IntentFilter(EVENT_ACTION_UPDATE);
    }

    public static IntentFilter getEventQueryFilter() {
        return new IntentFilter(EVENT_ACTION_QUERY);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
        broadcastManager = LocalBroadcastManager.getInstance(context);
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCES,Context.MODE_PRIVATE);
        databaseHelper = DatabaseHelper.getInstance(context);
        broadcastManager.registerReceiver(fetchDoneReceiver,FetchRunnable.getDoneFilter());
        registeredReceivers.add(fetchDoneReceiver);


        if(!executor.isShutdown()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    databaseHelper.clean();
                }
            });
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent == null) {
            return super.onStartCommand(intent,flags,startId);
        }

        processAction(intent);

        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        executor.shutdown();

        if(fetchRunnable != null) {
            fetchRunnable.interrupt();
        }

        for (BroadcastReceiver registeredReceiver : registeredReceivers) {
            broadcastManager.unregisterReceiver(registeredReceiver);
        }

        registeredReceivers.clear();
    }

    private void processAction(final Intent intent) {

        if(!executor.isShutdown()) {

            executor.execute(new Runnable() {
                @Override
                public void run() {

                    final long id = intent.getLongExtra(EXTRA_ID, DEFAULT_EMPTY_VALUE);

                    switch (intent.getIntExtra(ACTION_TYPE, DEFAULT_EMPTY_VALUE)) {

                        case ACTION_PAUSE: {
                            pause(id);
                            break;
                        }
                        case ACTION_REMOVE: {
                            remove(id);
                            break;
                        }
                        case ACTION_RESUME : {
                            resume(id);
                            break;
                        }
                        case ACTION_ENQUEUE : {
                            String url = intent.getStringExtra(EXTRA_URL);
                            String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
                            ArrayList<Bundle> headers = intent.getParcelableArrayListExtra(EXTRA_HEADERS);
                            int priority = intent.getIntExtra(EXTRA_PRIORITY,PRIORITY_NORMAL);

                            enqueue(url,filePath,headers,priority);
                            break;
                        }
                        case ACTION_NETWORK : {
                            int network = intent.getIntExtra(EXTRA_NETWORK_ID,NETWORK_ALL);
                            setAllowedNetwork(network);
                            break;
                        }
                        case ACTION_PROCESS_PENDING : {
                            startDownload();
                            break;
                        }
                        case ACTION_QUERY : {
                            long queryId = intent.getLongExtra(EXTRA_QUERY_ID,DEFAULT_EMPTY_VALUE);
                            int queryType = intent.getIntExtra(EXTRA_QUERY_TYPE,QUERY_ALL);
                            int status = intent.getIntExtra(EXTRA_STATUS,DEFAULT_EMPTY_VALUE);
                            query(queryType,queryId,id,status);
                            break;
                        }
                        case ACTION_PRIORITY: {
                            int priority = intent.getIntExtra(EXTRA_PRIORITY,PRIORITY_NORMAL);
                            setRequestPriority(id,priority);
                            break;
                        }
                        case ACTION_RETRY: {
                            retry(id);
                            break;
                        }
                        case ACTION_REMOVE_ALL: {
                            removeAll();
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            });
        }
    }

    private synchronized void startDownload() {

        if(removingRequest) {
            return;
        }

        databaseHelper.verifyOK();
        boolean networkAvailable = Utils.isNetworkAvailable(context);
        boolean onWiFi = Utils.isOnWiFi(context);

        if((!networkAvailable || (getAllowedNetwork() == NETWORK_WIFI && !onWiFi))
                && fetchRunnable != null) {

            fetchRunnable.interrupt();

        }else if(!fetchRunnableQueued) {

            fetchRunnableQueued = true;

            try {

                Cursor cursor = databaseHelper.getNextPending();

                if(cursor != null && !cursor.isClosed() && cursor.getCount() > 0) {

                    RequestInfo requestInfo = Utils.cursorToRequestInfo(cursor,true);

                    fetchRunnable = new FetchRunnable(context,requestInfo.getId(),
                            requestInfo.getUrl(), requestInfo.getFilePath()
                            ,requestInfo.getHeaders(),requestInfo.getFileSize());

                    new Thread(fetchRunnable).start();

                }else {
                    stopSelf();
                }

            }catch (Exception e) {
                e.printStackTrace();
                fetchRunnableQueued = false;
                startDownload();
            }
        }
    }

    private void enqueue(String url,String filePath,ArrayList<Bundle> headers,int priority) {

        try {

            if(url == null || filePath == null) {
                throw new EnqueueException("Request was not properly formatted. url:"
                        + url + ", filePath:" + filePath,ERROR_BAD_REQUEST);
            }

            if(Utils.fileExist(filePath)) {
                throw new EnqueueException("File already located at filePath: " + filePath
                        + ". The requested will not be enqueued.",ERROR_REQUEST_ALREADY_EXIST);
            }

            if(headers == null) {
                headers = new ArrayList<>();
            }

            long id = Utils.generateRequestId();
            String headerString = Utils.bundleListToHeaderString(headers);
            long fileSize = 0L;
            long downloadedBytes = 0L;

            boolean enqueued = databaseHelper.insert(id,url,filePath,STATUS_QUEUED,headerString,
                    downloadedBytes,fileSize,priority, DEFAULT_EMPTY_VALUE);

            if(!enqueued) {
                throw new EnqueueException("could not enqueue request",ERROR_ENQUEUE_ERROR);
            }

            sendEnqueueEvent(EVENT_ACTION_ENQUEUED,id,url,filePath,
                    STATUS_QUEUED,headers,priority,DEFAULT_EMPTY_VALUE);

        }catch (EnqueueException e) {

            sendEnqueueEvent(EVENT_ACTION_ENQUEUE_FAILED,DEFAULT_EMPTY_VALUE,
                    url,filePath,STATUS_NOT_QUEUED,headers,priority,e.getErrorCode());

        }finally {
            startDownload();
        }
    }

    private void resume(final long id) {

        if(fetchRunnable != null && fetchRunnable.getId() == id) {
            return;
        }

        boolean resumed = databaseHelper.resume(id);

        if(resumed) {

            Cursor cursor = databaseHelper.get(id);
            RequestInfo requestInfo = Utils.cursorToRequestInfo(cursor,true);

            if(requestInfo != null) {

                Utils.sendEventUpdate(broadcastManager, requestInfo.getId(),
                        requestInfo.getStatus(), requestInfo.getProgress(),
                        requestInfo.getDownloadedBytes(),requestInfo.getFileSize(),
                        requestInfo.getError());
            }
        }

        startDownload();
    }

    private void pause(final long id) {

        final boolean paused = databaseHelper.pause(id);

        if(paused && fetchRunnable != null && fetchRunnable.getId() == id) {

            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if(intent != null && FetchRunnable.getId(intent) == id) {

                        try {

                            Cursor cursor = databaseHelper.get(id);
                            RequestInfo requestInfo = Utils.cursorToRequestInfo(cursor,true);

                            if(requestInfo != null) {
                                Utils.sendEventUpdate(broadcastManager, requestInfo.getId(),
                                        requestInfo.getStatus(), requestInfo.getProgress(),
                                        requestInfo.getDownloadedBytes(),requestInfo.getFileSize(),
                                        requestInfo.getError());
                            }

                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    broadcastManager.unregisterReceiver(this);
                    registeredReceivers.remove(this);
                }
            };


            registeredReceivers.add(broadcastReceiver);
            broadcastManager.registerReceiver(broadcastReceiver,FetchRunnable.getDoneFilter());
            fetchRunnable.interrupt();
        }

        startDownload();
    }

    private void remove(final long id) {

        removingRequest = true;

        if (fetchRunnable != null && fetchRunnable.getId() == id) {
            fetchRunnable.interrupt();
        }

        Cursor cursor = databaseHelper.get(id);
        RequestInfo request = Utils.cursorToRequestInfo(cursor,true);

        boolean removed = databaseHelper.delete(id);

        if(removed && request != null) {

            Utils.deleteFile(request.getFilePath());

            Utils.sendEventUpdate(broadcastManager,id,
                    STATUS_REMOVED,0,0,0,DEFAULT_EMPTY_VALUE);
        }

        removingRequest = false;
        startDownload();
    }

    private void removeAll() {

        removingRequest = true;

        if (fetchRunnable != null) {
            fetchRunnable.interrupt();
        }

        Cursor cursor = databaseHelper.get();
        List<RequestInfo> requests = Utils.cursorToRequestInfoList(cursor,true);

        boolean removed = databaseHelper.deleteAll();

        if(requests != null && removed) {

            for (RequestInfo request : requests) {

                Utils.deleteFile(request.getFilePath());

                Utils.sendEventUpdate(broadcastManager,request.getId(),
                        STATUS_REMOVED,0,0,0,DEFAULT_EMPTY_VALUE);
            }
        }

        removingRequest = false;
        startDownload();
    }

    private void query(int queryType,long queryId, long requestId,int status) {

        Cursor cursor;

        switch (queryType) {
            case QUERY_SINGLE : {
                cursor = databaseHelper.get(requestId);
                break;
            }
            case QUERY_BY_STATUS : {
                cursor = databaseHelper.getByStatus(status);
                break;
            }
            default: {
                cursor = databaseHelper.get();
                break;
            }
        }

        ArrayList<Bundle> queryResults = Utils.cursorToQueryResultList(cursor,true);

        sendEventQuery(queryId,queryResults);

        startDownload();
    }

    private void setRequestPriority(long id, int priority) {

        boolean updated = databaseHelper.setPriority(id,priority);

        if(updated && fetchRunnable != null) {
            fetchRunnable.interrupt();
        }

        startDownload();
    }

    private void setAllowedNetwork(final int networkType) {

        sharedPreferences.edit().putInt(EXTRA_NETWORK_ID,networkType).apply();

        if(fetchRunnable != null) {
            fetchRunnable.interrupt();
        }

        startDownload();
    }

    private void retry(long id) {

        if(fetchRunnable != null && fetchRunnable.getId() == id) {
            return;
        }

        boolean retry = databaseHelper.retry(id);

        if(retry) {

            Cursor cursor = databaseHelper.get(id);
            RequestInfo requestInfo = Utils.cursorToRequestInfo(cursor,true);

            if(requestInfo != null) {
                Utils.sendEventUpdate(broadcastManager, requestInfo.getId(),
                        requestInfo.getStatus(), requestInfo.getProgress(),
                        requestInfo.getDownloadedBytes(),requestInfo.getFileSize(),
                        requestInfo.getError());
            }
        }

        startDownload();
    }

    private int getAllowedNetwork() {
        return sharedPreferences.getInt(EXTRA_NETWORK_ID, NETWORK_ALL);
    }

    private final BroadcastReceiver fetchDoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            fetchRunnableQueued = false;
            startDownload();
        }
    };

    private void sendEnqueueEvent(String action,long id, String url, String filePath,
                                  int status, ArrayList<Bundle> headers, int priority,
                                  int error) {

        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ID,id);
        intent.putExtra(EXTRA_STATUS,status);
        intent.putExtra(EXTRA_URL,url);
        intent.putExtra(EXTRA_FILE_PATH,filePath);
        intent.putExtra(EXTRA_HEADERS,headers);
        intent.putExtra(EXTRA_PROGRESS,0);
        intent.putExtra(EXTRA_FILE_SIZE,0L);
        intent.putExtra(EXTRA_ERROR,error);
        intent.putExtra(EXTRA_PRIORITY,priority);

        broadcastManager.sendBroadcast(intent);
    }

    private void sendEventQuery(long queryId,ArrayList<Bundle> results) {

        Intent intent = new Intent(FetchService.EVENT_ACTION_QUERY);

        intent.putExtra(FetchService.EXTRA_QUERY_ID,queryId);
        intent.putExtra(FetchService.EXTRA_QUERY_RESULT,results);

        broadcastManager.sendBroadcast(intent);
    }
}