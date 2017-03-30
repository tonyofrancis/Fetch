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

import android.os.Handler;
import android.os.Looper;

import com.tonyodev.fetch.callback.FetchCall;
import com.tonyodev.fetch.exception.DownloadInterruptedException;
import com.tonyodev.fetch.request.Header;
import com.tonyodev.fetch.request.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * FetchCallRunnable assists the Fetch
 * with downloading and reporting of a download request.
 *
 * @author Tonyo Francis
 */
final class FetchCallRunnable implements Runnable {

    private final Request request;
    private final FetchCall<String> fetchCall;
    private final Callback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private volatile boolean interrupted = false;
    private HttpURLConnection httpURLConnection;
    private InputStream input;
    private BufferedReader bufferedReader;
    private String response;


    FetchCallRunnable(Request request,FetchCall<String> fetchCall,Callback callback) {

        if(request == null) {
            throw new NullPointerException("Request Cannot be null");
        }

        if(fetchCall == null) {
            throw new NullPointerException("FetchCall cannot be null");
        }

        if(callback == null) {
            throw new NullPointerException("Callback cannot be null");
        }

        this.request = request;
        this.fetchCall = fetchCall;
        this.callback = callback;
    }

    @Override
    public void run() {

        try {

            setHttpConnectionPrefs();
            httpURLConnection.connect();
            int responseCode = httpURLConnection.getResponseCode();

            if(responseCode == HttpURLConnection.HTTP_OK) {

                if (isInterrupted()) {
                    throw new DownloadInterruptedException("DIE",ErrorUtils.DOWNLOAD_INTERRUPTED);
                }

                input = httpURLConnection.getInputStream();
                response = inputToString();

                if(!isInterrupted()) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            fetchCall.onSuccess(response,request);
                        }
                    });
                }
            }else {
                throw new IllegalStateException("SSRV:" + responseCode);
            }
        }catch (Exception exception) {
            exception.printStackTrace();

            final int error = ErrorUtils.getCode(exception.getMessage());

            if(!isInterrupted()) {

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        fetchCall.onError(error,request);
                    }
                });
            }
        }finally {
            release();
            callback.onDone(request);
        }
    }

    private void setHttpConnectionPrefs() throws IOException {

        URL httpUrl = new URL(request.getUrl());
        httpURLConnection = (HttpURLConnection) httpUrl.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setReadTimeout(15_000);
        httpURLConnection.setConnectTimeout(10_000);
        httpURLConnection.setUseCaches(true);
        httpURLConnection.setDefaultUseCaches(true);
        httpURLConnection.setInstanceFollowRedirects(true);
        httpURLConnection.setDoInput(true);

        for (Header header : request.getHeaders()) {
            httpURLConnection.addRequestProperty(header.getHeader(),header.getValue());
        }
    }

    private String inputToString() throws IOException {

        StringBuilder stringBuilder = new StringBuilder();

        String line;
        bufferedReader = new BufferedReader(new InputStreamReader(input));

        while ((line = bufferedReader.readLine()) != null && !isInterrupted()) {
            stringBuilder.append(line);
        }

        if(isInterrupted()) {
            return null;
        }

        return stringBuilder.toString();
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
            if(bufferedReader != null) {
                bufferedReader.close();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        if (httpURLConnection != null) {
            httpURLConnection.disconnect();
        }
    }

    private boolean isInterrupted() {
        return interrupted;
    }

    synchronized void interrupt() {
        this.interrupted = true;
    }

    public Request getRequest() {
        return request;
    }

    interface Callback {
        void onDone(Request request);
    }
}