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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
    public static final String EXTRA_LOGGING_ID = "com.tonyodev.fetch.extra_logging_id";
    public static final String EXTRA_CONCURRENT_DOWNLOADS_LIMIT = "com.tonyodev.fetch.extra_concurrent_download_limit";
    public static final String EXTRA_ON_UPDATE_INTERVAL = "com.tonyodev.fetch.extra_on_update_interval";

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
    public static final int ACTION_LOGGING = 320;
    public static final int ACTION_CONCURRENT_DOWNLOADS_LIMIT = 321;
    public static final int ACTION_UPDATE_REQUEST_URL = 322;
    public static final int ACTION_ON_UPDATE_INTERVAL = 323;
    public static final int ACTION_REMOVE_REQUEST = 324;
    public static final int ACTION_REMOVE_REQUEST_ALL = 325;


    public static final int QUERY_SINGLE = 480;
    public static final int QUERY_ALL = 481;
    public static final int QUERY_BY_STATUS = 482;

    private static final String SHARED_PREFERENCES = "com.tonyodev.fetch.shared_preferences";

    private Context context;
    private DatabaseHelper databaseHelper;
    private LocalBroadcastManager broadcastManager;
    private SharedPreferences sharedPreferences;

    private volatile boolean runningTask = false;
    private volatile boolean shuttingDown = false;
    private int downloadsLimit = DEFAULT_DOWNLOADS_LIMIT;
    private boolean loggingEnabled = true;
    private long onUpdateInterval = DEFAULT_ON_UPDATE_INTERVAL;
    private int preferredNetwork = NETWORK_ALL;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<BroadcastReceiver> registeredReceivers = new ArrayList<>();
    private final ConcurrentHashMap<Long,FetchRunnable> activeDownloads = new ConcurrentHashMap<>();

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

    @NonNull
    public static IntentFilter getEventEnqueuedFilter() {
        return new IntentFilter(EVENT_ACTION_ENQUEUED);
    }

    @NonNull
    public static IntentFilter getEventEnqueueFailedFilter() {
        return new IntentFilter(EVENT_ACTION_ENQUEUE_FAILED);
    }

    @NonNull
    public static IntentFilter getEventUpdateFilter() {
        return new IntentFilter(EVENT_ACTION_UPDATE);
    }

    @NonNull
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
        broadcastManager.registerReceiver(doneReceiver,FetchRunnable.getDoneFilter());
        registeredReceivers.add(doneReceiver);
        downloadsLimit = getDownloadsLimit();
        preferredNetwork = getAllowedNetwork();
        loggingEnabled = isLoggingEnabled();
        onUpdateInterval = getOnUpdateInterval();
        databaseHelper.setLoggingEnabled(loggingEnabled);

        if(!executor.isShutdown()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    databaseHelper.clean();
                    databaseHelper.verifyOK();
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

        shuttingDown = true;

        if(!executor.isShutdown()){
            executor.shutdown();
        }

        interruptActiveDownloads();

        for (BroadcastReceiver registeredReceiver : registeredReceivers) {
            broadcastManager.unregisterReceiver(registeredReceiver);
        }

        registeredReceivers.clear();
    }

    private void processAction(final Intent intent) {

        if(intent == null) {
            return;
        }

        if(!executor.isShutdown()) {

            executor.execute(new Runnable() {
                @Override
                public void run() {

                    databaseHelper.clean();
                    
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
                        case ACTION_LOGGING : {
                            boolean enabled = intent.getBooleanExtra(EXTRA_LOGGING_ID,true);
                            setLoggingEnabled(enabled);
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
                        case ACTION_CONCURRENT_DOWNLOADS_LIMIT: {
                            int limit = intent.getIntExtra(EXTRA_CONCURRENT_DOWNLOADS_LIMIT,DEFAULT_DOWNLOADS_LIMIT);
                            setDownloadsLimit(limit);
                            break;
                        }
                        case ACTION_ON_UPDATE_INTERVAL: {
                            long interval = intent.getLongExtra(EXTRA_ON_UPDATE_INTERVAL, DEFAULT_ON_UPDATE_INTERVAL);
                            setOnUpdateInterval(interval);
                            break;
                        }
                        case ACTION_UPDATE_REQUEST_URL: {
                            String url = intent.getStringExtra(EXTRA_URL);
                            updateRequestUrl(id,url);
                            break;
                        }
                        case ACTION_REMOVE_REQUEST: {
                            removeRequest(id);
                            break;
                        }
                        case ACTION_REMOVE_REQUEST_ALL: {
                            removeRequestAll();
                            break;
                        }
                        default: {
                            startDownload();
                            break;
                        }
                    }
                }
            });
        }
    }

    private synchronized void startDownload() {

        if(shuttingDown || runningTask) {
            return;
        }

        boolean networkAvailable = Utils.isNetworkAvailable(context);
        boolean onWiFi = Utils.isOnWiFi(context);

        if((!networkAvailable || (preferredNetwork == NETWORK_WIFI && !onWiFi)) && activeDownloads.size() > 0) {

            runningTask = true;
            interruptActiveDownloads();
            runningTask = false;

        }else if(networkAvailable && !runningTask && activeDownloads.size() < downloadsLimit
                && databaseHelper.hasPendingRequests()) {

            runningTask = true;

            try {

                Cursor cursor = databaseHelper.getNextPendingRequest();

                if(cursor != null && !cursor.isClosed() && cursor.getCount() > 0) {

                    RequestInfo requestInfo = Utils.cursorToRequestInfo(cursor,true,loggingEnabled);

                    FetchRunnable fetchRunnable = new FetchRunnable(context,requestInfo.getId(),
                            requestInfo.getUrl(), requestInfo.getFilePath()
                            ,requestInfo.getHeaders(),requestInfo.getFileSize(),loggingEnabled, onUpdateInterval);

                    databaseHelper.updateStatus(requestInfo.getId(),FetchService.STATUS_DOWNLOADING,DEFAULT_EMPTY_VALUE);
                    activeDownloads.put(fetchRunnable.getId(),fetchRunnable);

                    new Thread(fetchRunnable).start();
                }

            }catch (Exception e) {

                if(loggingEnabled) {
                    e.printStackTrace();
                }
            }

            runningTask = false;

            if(activeDownloads.size() < downloadsLimit && databaseHelper.hasPendingRequests()) {
                startDownload();
            }
        }else if(!runningTask && activeDownloads.size() == 0 && !databaseHelper.hasPendingRequests()) {
            shuttingDown = true;
            stopSelf();
        }
    }

    private void interruptActiveDownloads() {

        for (Long id : activeDownloads.keySet()) {

            FetchRunnable fetchRunnable = activeDownloads.get(id);

            if(fetchRunnable != null) {
                fetchRunnable.interrupt();
            }
        }
    }

    private void interruptActiveDownload(long id) {

        if(activeDownloads.containsKey(id)) {
            FetchRunnable fetchRunnable = activeDownloads.get(id);

            if(fetchRunnable != null) {
                fetchRunnable.interrupt();
            }
        }
    }

    private void enqueue(String url,String filePath,ArrayList<Bundle> headers,int priority) {

        try {

            if(url == null || filePath == null) {
                throw new EnqueueException("Request was not properly formatted. url:"
                        + url + ", filePath:" + filePath,ERROR_BAD_REQUEST);
            }

            if(headers == null) {
                headers = new ArrayList<>();
            }

            long id = Utils.generateRequestId();
            String headerString = Utils.bundleListToHeaderString(headers,loggingEnabled);
            long fileSize = 0L;
            long downloadedBytes = 0L;

            File file = Utils.getFile(filePath);

            if (file.exists()) {
                downloadedBytes = file.length();
            }

            boolean enqueued = databaseHelper.insert(id,url,filePath,STATUS_QUEUED,headerString,
                    downloadedBytes,fileSize,priority, DEFAULT_EMPTY_VALUE);

            if(!enqueued) {
                throw new EnqueueException("could not enqueue request",ERROR_ENQUEUE_ERROR);
            }

            sendEnqueueEvent(EVENT_ACTION_ENQUEUED,id,url,filePath,
                    STATUS_QUEUED,headers,priority,DEFAULT_EMPTY_VALUE);

        }catch (EnqueueException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }

            sendEnqueueEvent(EVENT_ACTION_ENQUEUE_FAILED,DEFAULT_EMPTY_VALUE,
                    url,filePath,STATUS_NOT_QUEUED,headers,priority,e.getErrorCode());

        }finally {
            startDownload();
        }
    }

    private void resume(final long id) {

        if(activeDownloads.containsKey(id)) {
            return;
        }

        boolean resumed = databaseHelper.resume(id);

        if(resumed) {

            Cursor cursor = databaseHelper.get(id);
            RequestInfo requestInfo = Utils.cursorToRequestInfo(cursor,true,loggingEnabled);

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

        if(activeDownloads.containsKey(id)) {

            runningTask = true;

            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if(FetchRunnable.getIdFromIntent(intent) == id) {
                        pauseAction(id);

                        broadcastManager.unregisterReceiver(this);
                        registeredReceivers.remove(this);
                        runningTask = false;
                        startDownload();
                    }
                }
            };

            registeredReceivers.add(broadcastReceiver);
            broadcastManager.registerReceiver(broadcastReceiver,FetchRunnable.getDoneFilter());
            interruptActiveDownload(id);
        }else {

            pauseAction(id);
            startDownload();
        }
    }

    private void pauseAction(long id) {

        if(databaseHelper.pause(id)) {

            Cursor cursor = databaseHelper.get(id);
            RequestInfo requestInfo = Utils.cursorToRequestInfo(cursor,true,loggingEnabled);

            if(requestInfo != null) {

                Utils.sendEventUpdate(broadcastManager, requestInfo.getId(),
                        requestInfo.getStatus(), requestInfo.getProgress(),
                        requestInfo.getDownloadedBytes(),requestInfo.getFileSize(),
                        requestInfo.getError());
            }

        }
    }

    private void remove(final long id) {

        if (activeDownloads.containsKey(id)) {

            runningTask = true;

            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if(FetchRunnable.getIdFromIntent(intent) == id) {
                        removeAction(id);

                        broadcastManager.unregisterReceiver(this);
                        registeredReceivers.remove(this);
                        runningTask = false;
                        startDownload();
                    }
                }
            };

            registeredReceivers.add(broadcastReceiver);
            broadcastManager.registerReceiver(broadcastReceiver,FetchRunnable.getDoneFilter());
            interruptActiveDownload(id);
        }else {
            removeAction(id);
            startDownload();
        }
    }

    private void removeAction(long id) {

        Cursor cursor = databaseHelper.get(id);
        RequestInfo request = Utils.cursorToRequestInfo(cursor,true,loggingEnabled);

        if(request != null && databaseHelper.delete(id)) {

            Utils.deleteFile(request.getFilePath());

            Utils.sendEventUpdate(broadcastManager,id,
                    STATUS_REMOVED,0,0,0,DEFAULT_EMPTY_VALUE);
        }
    }

    private void removeAll() {

        if (activeDownloads.size() > 0) {

            runningTask = true;

            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if(intent != null) {
                        long id = FetchRunnable.getIdFromIntent(intent);
                        removeAction(id);
                    }

                    if(activeDownloads.size() == 0) {
                        removeAllAction();
                        broadcastManager.unregisterReceiver(this);
                        registeredReceivers.remove(this);
                        runningTask = false;
                        startDownload();
                    }
                }
            };

            registeredReceivers.add(broadcastReceiver);
            broadcastManager.registerReceiver(broadcastReceiver,FetchRunnable.getDoneFilter());
            interruptActiveDownloads();
        }else {
            removeAllAction();
            startDownload();
        }
    }

    private void removeAllAction() {

        Cursor cursor = databaseHelper.get();
        List<RequestInfo> requests = Utils.cursorToRequestInfoList(cursor,true,loggingEnabled);

        if(requests != null && databaseHelper.deleteAll()) {

            for (RequestInfo request : requests) {

                Utils.deleteFile(request.getFilePath());

                Utils.sendEventUpdate(broadcastManager,request.getId(),
                        STATUS_REMOVED,0,0,0,DEFAULT_EMPTY_VALUE);
            }
        }
    }

    private void removeRequestAll() {

        if (activeDownloads.size() > 0) {

            runningTask = true;

            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if(intent != null) {
                        long id = FetchRunnable.getIdFromIntent(intent);
                        removeRequestAction(id);
                    }

                    if(activeDownloads.size() == 0) {
                        removeRequestAllAction();
                        broadcastManager.unregisterReceiver(this);
                        registeredReceivers.remove(this);
                        runningTask = false;
                        startDownload();
                    }
                }
            };

            registeredReceivers.add(broadcastReceiver);
            broadcastManager.registerReceiver(broadcastReceiver,FetchRunnable.getDoneFilter());
            interruptActiveDownloads();
        }else {
            removeRequestAllAction();
            startDownload();
        }
    }

    private void removeRequestAllAction() {

        Cursor cursor = databaseHelper.get();
        List<RequestInfo> requests = Utils.cursorToRequestInfoList(cursor,true,loggingEnabled);

        if(requests != null && databaseHelper.deleteAll()) {

            for (RequestInfo request : requests) {

                Utils.sendEventUpdate(broadcastManager,request.getId(),
                        STATUS_REMOVED,request.getProgress(),request.getDownloadedBytes(),
                        request.getFileSize(),DEFAULT_EMPTY_VALUE);
            }
        }
    }

    private void removeRequest(final long id) {

        if (activeDownloads.containsKey(id)) {

            runningTask = true;

            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if(FetchRunnable.getIdFromIntent(intent) == id) {
                        removeRequestAction(id);

                        broadcastManager.unregisterReceiver(this);
                        registeredReceivers.remove(this);
                        runningTask = false;
                        startDownload();
                    }
                }
            };

            registeredReceivers.add(broadcastReceiver);
            broadcastManager.registerReceiver(broadcastReceiver,FetchRunnable.getDoneFilter());
            interruptActiveDownload(id);
        }else {
            removeRequestAction(id);
            startDownload();
        }
    }

    private void removeRequestAction(long id) {

        Cursor cursor = databaseHelper.get(id);
        RequestInfo request = Utils.cursorToRequestInfo(cursor,true,loggingEnabled);

        if(request != null && databaseHelper.delete(id)) {

            Utils.sendEventUpdate(broadcastManager,id,
                    STATUS_REMOVED,request.getProgress(),request.getDownloadedBytes(),
                    request.getFileSize(),DEFAULT_EMPTY_VALUE);
        }
    }

    private void query(int queryType,long queryId,long requestId,int status) {

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

        ArrayList<Bundle> queryResults = Utils.cursorToQueryResultList(cursor,true,loggingEnabled);
        sendEventQuery(queryId,queryResults);
        startDownload();
    }

    private void setRequestPriority(long id, int priority) {

        if(databaseHelper.setPriority(id,priority) && activeDownloads.size() > 0) {
            interruptActiveDownloads();
        }

        startDownload();
    }

    private void setAllowedNetwork(final int networkType) {

        preferredNetwork = networkType;
        sharedPreferences.edit().putInt(EXTRA_NETWORK_ID,networkType).apply();

        if(activeDownloads.size() > 0) {
            interruptActiveDownloads();
        }

        startDownload();
    }

    private void retry(long id) {

        if(activeDownloads.containsKey(id)) {
            return;
        }

        if(databaseHelper.retry(id)) {

            Cursor cursor = databaseHelper.get(id);
            RequestInfo requestInfo = Utils.cursorToRequestInfo(cursor,true,loggingEnabled);

            if(requestInfo != null) {
                Utils.sendEventUpdate(broadcastManager, requestInfo.getId(),
                        requestInfo.getStatus(), requestInfo.getProgress(),
                        requestInfo.getDownloadedBytes(),requestInfo.getFileSize(),
                        requestInfo.getError());
            }
        }

        startDownload();
    }

    private void updateRequestUrl(final long id,final String url) {

        if (activeDownloads.containsKey(id)) {

            runningTask = true;

            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if(FetchRunnable.getIdFromIntent(intent) == id) {
                        updateRequestUrlAction(id,url);

                        broadcastManager.unregisterReceiver(this);
                        registeredReceivers.remove(this);
                        runningTask = false;
                        startDownload();
                    }
                }
            };

            registeredReceivers.add(broadcastReceiver);
            broadcastManager.registerReceiver(broadcastReceiver,FetchRunnable.getDoneFilter());
            interruptActiveDownload(id);
        }else {
            updateRequestUrlAction(id,url);
            startDownload();
        }
    }

    private void updateRequestUrlAction(long id,String url) {

        databaseHelper.updateUrl(id,url);
        databaseHelper.retry(id);
    }

    private int getAllowedNetwork() {
        return sharedPreferences.getInt(EXTRA_NETWORK_ID, NETWORK_ALL);
    }

    private final BroadcastReceiver doneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent != null) {

                long id = FetchRunnable.getIdFromIntent(intent);

                if(activeDownloads.containsKey(id)) {
                    activeDownloads.remove(id);
                }

                startDownload();
            }
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

    private int getDownloadsLimit() {
        return sharedPreferences.getInt(EXTRA_CONCURRENT_DOWNLOADS_LIMIT,DEFAULT_DOWNLOADS_LIMIT);
    }

    private void setDownloadsLimit(int limit) {

        if(limit < DEFAULT_DOWNLOADS_LIMIT) {
            limit = DEFAULT_DOWNLOADS_LIMIT;
        }

        downloadsLimit = limit;
        sharedPreferences.edit().putInt(EXTRA_CONCURRENT_DOWNLOADS_LIMIT,limit).apply();

        if(activeDownloads.size() > 0) {
            interruptActiveDownloads();
        }

        startDownload();
    }

    private void setLoggingEnabled(boolean enabled) {

        loggingEnabled = enabled;
        sharedPreferences.edit().putBoolean(EXTRA_LOGGING_ID,enabled).apply();
        databaseHelper.setLoggingEnabled(loggingEnabled);
        startDownload();
    }

    private boolean isLoggingEnabled() {
        return sharedPreferences.getBoolean(EXTRA_LOGGING_ID,true);
    }

    static boolean isLoggingEnabled(Context context) {
        return  context.getSharedPreferences(SHARED_PREFERENCES,Context.MODE_PRIVATE)
                .getBoolean(EXTRA_LOGGING_ID,true);
    }

    private void setOnUpdateInterval(long intervalMs) {
        onUpdateInterval = intervalMs;
        sharedPreferences.edit().putLong(EXTRA_ON_UPDATE_INTERVAL, intervalMs).apply();

        if(activeDownloads.size() > 0) {
            interruptActiveDownloads();
        }

        startDownload();
    }

    private long getOnUpdateInterval() {
        onUpdateInterval = sharedPreferences.getLong(EXTRA_ON_UPDATE_INTERVAL, DEFAULT_ON_UPDATE_INTERVAL);
        return onUpdateInterval;
    }
}