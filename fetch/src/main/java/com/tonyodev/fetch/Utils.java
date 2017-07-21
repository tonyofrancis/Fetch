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
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.tonyodev.fetch.callback.FetchTask;
import com.tonyodev.fetch.exception.InvalidStatusException;
import com.tonyodev.fetch.exception.NotUsableException;
import com.tonyodev.fetch.request.Header;
import com.tonyodev.fetch.request.RequestInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Utility class used by Fetch and the FetchService
 *
 * @author Tonyo Francis
 * */
final class Utils {

    private Utils() {
    }

    static void throwIfFetchTaskNull(FetchTask fetchTask) {

        if(fetchTask == null) {
            throw new NullPointerException("FetchTask cannot be null");
        }
    }

    static void throwIfInvalidStatus(int status) {

        switch (status) {
            case FetchConst.STATUS_QUEUED:
            case FetchConst.STATUS_DOWNLOADING:
            case FetchConst.STATUS_PAUSED:
            case FetchConst.STATUS_DONE:
            case FetchConst.STATUS_ERROR:
            case FetchConst.STATUS_REMOVED:
            case FetchConst.STATUS_NOT_QUEUED:
                return;
            default:
                throw new InvalidStatusException(status + " is not a valid status "
                        ,ErrorUtils.INVALID_STATUS);
        }
    }
    
    static boolean hasIntervalElapsed(long startTime, long stopTime, long onUpdateInterval) {

        if(TimeUnit.NANOSECONDS.toMillis(stopTime - startTime) >= onUpdateInterval) {
            return true;
        }

        return false;
    }

    static int getProgress(long downloadedBytes,long fileSize) {

        if (fileSize < 1 || downloadedBytes < 1) {
            return  0;
        } else if(downloadedBytes >= fileSize) {
            return 100;
        } else {
            return (int) (((double) downloadedBytes / (double) fileSize) * 100);
        }
    }

    static String headerListToString(List<Header> headers,boolean loggingEnabled) {

        if(headers == null) {
            return "{}";
        }

        String headerString;

        try {

            JSONObject headerObject = new JSONObject();

            for (Header header : headers) {
                headerObject.put(header.getHeader(),header.getValue());
            }

            headerString = headerObject.toString();
        }catch (JSONException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }

            headerString = "{}";
        }

        return headerString;
    }

    static List<Header> headerStringToList(String headers,boolean loggingEnabled) {

        List<Header> headerList = new ArrayList<>();

        try {

            JSONObject jsonObject = new JSONObject(headers);
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                headerList.add(new Header(key,jsonObject.getString(key)));
            }

        }catch (JSONException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        return headerList;
    }

    static boolean isOnWiFi(Context context) {

        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if(activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            return activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }

        return false;
    }

    static boolean isNetworkAvailable(Context context) {

        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    static boolean createFileIfNotExist(String path) throws IOException, NullPointerException {

        File file = new File(path);

        boolean created;

        if(!file.exists()) {
            created = file.createNewFile();
        } else {
            created = true;
        }

        return created;
    }

    static boolean createDirIfNotExist(String path) throws NullPointerException {

        boolean created;
        File dir = new File(path);

        if(!dir.exists()) {
            created = dir.mkdirs();
        } else {
            created = true;
        }
        return created;
    }

    static boolean deleteFile(String filePath) {

        return new File(filePath).delete();
    }

    static long getFileSize(String filePath) {

        return new File(filePath).length();
    }

    static boolean fileExist(String filePath) {

        return new File(filePath).exists();
    }

    static File getFile(String filePath) {
        return new File(filePath);
    }

    static void createFileOrThrow(String filePath) throws IOException,NullPointerException {

        File file = Utils.getFile(filePath);
        boolean parentDirCreated = Utils.createDirIfNotExist(file.getParentFile().getAbsolutePath());
        boolean fileCreated = Utils.createFileIfNotExist(file.getAbsolutePath());

        if(!parentDirCreated || !fileCreated) {
            throw new IOException("File could not be created for the filePath:" + filePath);
        }
    }

    static void throwIfNotUsable(Fetch fetch) {

        if(fetch == null) {
            throw new NullPointerException("Fetch cannot be null");
        }

        if(fetch.isReleased()) {
            throw new NotUsableException("Fetch instance: "
                    + fetch.toString() + " cannot be reused after calling its release() method." +
                    "Call Fetch.getInstance() for a new instance of Fetch.",ErrorUtils.NOT_USABLE);
        }
    }

   static RequestInfo cursorToRequestInfo(Cursor cursor, boolean closeCursor,boolean loggingEnabled) {

       RequestInfo requestInfo = null;

       try {

           if(cursor == null || cursor.isClosed() || cursor.getCount() < 1) {
               return requestInfo;
           }

           cursor.moveToFirst();
           requestInfo = createRequestInfo(cursor,loggingEnabled);
       }catch (Exception e) {

           if(loggingEnabled) {
               e.printStackTrace();
           }
       }finally {
           if(cursor != null && closeCursor) {
               cursor.close();
           }
       }

       return requestInfo;
    }

    static List<RequestInfo> cursorToRequestInfoList(Cursor cursor, boolean closeCursor,boolean loggingEnabled) {

        List<RequestInfo> requests = new ArrayList<>();

        try {

            if(cursor == null || cursor.isClosed() || cursor.getCount() < 1) {
                return requests;
            }

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {

                requests.add(createRequestInfo(cursor,loggingEnabled));
                cursor.moveToNext();
            }

        }catch (Exception e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }finally {
            if(cursor != null && closeCursor) {
                cursor.close();
            }
        }

        return requests;
    }

    static RequestInfo createRequestInfo(Cursor cursor,boolean loggingEnabled) {

        if(cursor == null || cursor.isClosed() || cursor.getCount() < 1) {
            return null;
        }

        long id = cursor.getLong(DatabaseHelper.INDEX_COLUMN_ID);
        int status = cursor.getInt(DatabaseHelper.INDEX_COLUMN_STATUS);
        String url = cursor.getString(DatabaseHelper.INDEX_COLUMN_URL);
        String filePath = cursor.getString(DatabaseHelper.INDEX_COLUMN_FILEPATH);
        int error = cursor.getInt(DatabaseHelper.INDEX_COLUMN_ERROR);
        long fileSize = cursor.getLong(DatabaseHelper.INDEX_COLUMN_FILE_SIZE);
        int priority = cursor.getInt(DatabaseHelper.INDEX_COLUMN_PRIORITY);
        long downloadedBytes = cursor.getLong(DatabaseHelper.INDEX_COLUMN_DOWNLOADED_BYTES);

        String headers = cursor.getString(DatabaseHelper.INDEX_COLUMN_HEADERS);
        List<Header> headersList = headerStringToList(headers,loggingEnabled);


        int progress = getProgress(downloadedBytes,fileSize);

        return new RequestInfo(id,status,url,filePath,progress,
                downloadedBytes,fileSize,error,headersList,priority);

    }

    static ArrayList<Bundle> cursorToQueryResultList(Cursor cursor, boolean closeCursor,boolean loggingEnabled) {

        ArrayList<Bundle> requests = new ArrayList<>();

        try {

            if(cursor == null || cursor.isClosed()) {
                return requests;
            }

            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {

                long id = cursor.getLong(DatabaseHelper.INDEX_COLUMN_ID);
                int status = cursor.getInt(DatabaseHelper.INDEX_COLUMN_STATUS);
                String url = cursor.getString(DatabaseHelper.INDEX_COLUMN_URL);
                String filePath = cursor.getString(DatabaseHelper.INDEX_COLUMN_FILEPATH);
                int error = cursor.getInt(DatabaseHelper.INDEX_COLUMN_ERROR);
                long fileSize = cursor.getLong(DatabaseHelper.INDEX_COLUMN_FILE_SIZE);
                int priority = cursor.getInt(DatabaseHelper.INDEX_COLUMN_PRIORITY);
                long downloadedBytes = cursor.getLong(DatabaseHelper.INDEX_COLUMN_DOWNLOADED_BYTES);

                String headers = cursor.getString(DatabaseHelper.INDEX_COLUMN_HEADERS);
                ArrayList<Bundle> headersList = headersToBundleList(headers,loggingEnabled);

                int progress = getProgress(downloadedBytes,fileSize);

                Bundle bundle = new Bundle();
                bundle.putLong(FetchService.EXTRA_ID,id);
                bundle.putInt(FetchService.EXTRA_STATUS,status);
                bundle.putString(FetchService.EXTRA_URL,url);
                bundle.putString(FetchService.EXTRA_FILE_PATH,filePath);
                bundle.putInt(FetchService.EXTRA_ERROR,error);
                bundle.putLong(FetchService.EXTRA_DOWNLOADED_BYTES,downloadedBytes);
                bundle.putLong(FetchService.EXTRA_FILE_SIZE,fileSize);
                bundle.putInt(FetchService.EXTRA_PROGRESS,progress);
                bundle.putInt(FetchService.EXTRA_PRIORITY,priority);
                bundle.putParcelableArrayList(FetchService.EXTRA_HEADERS,headersList);

                requests.add(bundle);

                cursor.moveToNext();
            }
        }catch (Exception e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }finally {
            if(cursor != null && closeCursor) {
                cursor.close();
            }
        }

        return requests;
    }


    static long generateRequestId() {
        return System.nanoTime();
    }

    static void sendEventUpdate(LocalBroadcastManager broadcastManager,long id,
                                int status,int progress,long downloadedBytes,long fileSize,int error) {

        if(broadcastManager == null) {
            return;
        }

        Intent intent = new Intent(FetchService.EVENT_ACTION_UPDATE);
        intent.putExtra(FetchService.EXTRA_ID,id);
        intent.putExtra(FetchService.EXTRA_STATUS,status);
        intent.putExtra(FetchService.EXTRA_PROGRESS,progress);
        intent.putExtra(FetchService.EXTRA_DOWNLOADED_BYTES,downloadedBytes);
        intent.putExtra(FetchService.EXTRA_FILE_SIZE,fileSize);
        intent.putExtra(FetchService.EXTRA_ERROR,error);

        broadcastManager.sendBroadcast(intent);
    }

    static ArrayList<Bundle> headersToBundleList(String headers,boolean loggingEnabled) {

        ArrayList<Bundle> headerList = new ArrayList<>();

        if(headers == null) {
            return headerList;
        }

        try {

            JSONObject jsonObject = new JSONObject(headers);
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();

                Bundle bundle = new Bundle();
                bundle.putString(FetchService.EXTRA_HEADER_NAME,key);
                bundle.putString(FetchService.EXTRA_HEADER_VALUE,jsonObject.getString(key));

                headerList.add(bundle);
            }

        }catch (JSONException e) {

            if(loggingEnabled) {
                e.printStackTrace();
            }
        }

        return headerList;
    }

    static String bundleListToHeaderString(List<Bundle> headers,boolean loggingEnabled) {

        String headerString;

        if(headers == null) {
            headerString = "{}";
        } else {
            JSONObject headerObject = new JSONObject();
            try {

                for (Bundle headerBundle: headers) {

                    String headerName = headerBundle.getString(FetchService.EXTRA_HEADER_NAME);
                    String headerValue = headerBundle.getString(FetchService.EXTRA_HEADER_VALUE);

                    if(headerValue == null) {
                        headerValue = "";
                    }

                    if(headerName != null) {
                        headerObject.put(headerName,headerValue);
                    }
                }

                headerString = headerObject.toString();
            }catch (JSONException e) {

                if(loggingEnabled) {
                    e.printStackTrace();
                }

                headerString = "{}";
            }
        }

        return headerString;
    }

    static boolean containsRequest(Cursor cursor,boolean closeCursor) {

        if(cursor != null && cursor.getCount() > 0) {

            if(closeCursor) {
                cursor.close();
            }

            return true;
        }

        return false;
    }

    static void throwIfInvalidUrl(String url) {

        String scheme = Uri.parse(url).getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + url);
        }
    }
}