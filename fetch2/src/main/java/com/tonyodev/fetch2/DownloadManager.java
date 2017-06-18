package com.tonyodev.fetch2;


import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;


final class DownloadManager implements Disposable {

    private final Context context;
    private final OkHttpClient okHttpClient;
    private final DatabaseManager databaseManager;
    private final DownloadListener downloadListener;
    private final ConcurrentHashMap<Long,DownloadRunnable> downloadsMap;
    private volatile boolean isDisposed;
    private final ActionProcessor<Runnable> actionProcessor;

    static DownloadManager newInstance(Context context, DatabaseManager databaseManager,
                                       OkHttpClient client, DownloadListener downloadListener, ActionProcessor<Runnable> actionProcessor) {
        return new DownloadManager(context,databaseManager,client,downloadListener,actionProcessor);
    }

    private DownloadManager(Context context,DatabaseManager databaseManager,
                            OkHttpClient client,DownloadListener downloadListener,
                            ActionProcessor<Runnable> actionProcessor) {
        this.isDisposed = false;
        this.context = context;
        this.databaseManager = databaseManager;
        this.okHttpClient = client;
        this.downloadListener = downloadListener;
        this.downloadsMap = new ConcurrentHashMap<>();
        this.actionProcessor = actionProcessor;
    }

    void pause(long id) {
        if(isDisposed) {
            return;
        }

        interrupt(id, InterruptReason.PAUSED);
        actionProcessor.processNext();
    }

    void pauseAll() {
        if (isDisposed) {
            return;
        }

        interruptAll(InterruptReason.PAUSED);
        actionProcessor.processNext();
    }

    void resume(final long id) {
        if(isDisposed) {
            return;
        }

        databaseManager.executeTransaction(new Transaction() {

            @Override
            public void onPreExecute() {

            }

            @Override
            public void onExecute(Database database) {
                if(!downloadsMap.containsKey(id)){
                    RequestData requestData = database.query(id);
                    if(requestData != null && DownloadHelper.canRetry(requestData.getStatus())) {
                        database.setStatusAndError(id, Status.DOWNLOADING, Error.NONE.getValue());
                        download(requestData);
                    }
                }
            }

            @Override
            public void onPostExecute() {

            }
        });

        actionProcessor.processNext();
    }

    void resumeAll() {
        if (isDisposed) {
            return;
        }

        databaseManager.executeTransaction(new Transaction() {

            @Override
            public void onPreExecute() {

            }

            @Override
            public void onExecute(Database database) {
                List<RequestData> requestDataList = database.query();

                for (RequestData requestData : requestDataList) {

                    if(!downloadsMap.containsKey(requestData.getId())
                            && DownloadHelper.canRetry(requestData.getStatus())){
                        database.setStatusAndError(requestData.getId(), Status.DOWNLOADING, Error.NONE.getValue());
                        download(requestData);
                    }
                }
            }

            @Override
            public void onPostExecute() {

            }
        });

        actionProcessor.processNext();
    }

    void retry(long id) {
        resume(id);
    }

    void cancel(final long id) {
        if (isDisposed) {
            return;
        }

        databaseManager.executeTransaction(new Transaction() {

            @Override
            public void onPreExecute() {

            }

            @Override
            public void onExecute(Database database) {
                RequestData requestData = database.query(id);

                if(requestData != null && DownloadHelper.canCancel(requestData.getStatus())) {

                    if(downloadsMap.containsKey(requestData.getId())) {
                        interrupt(requestData.getId(), InterruptReason.CANCELLED);
                    }else {
                        database.setStatusAndError(id, Status.CANCELLED, Error.NONE.getValue());
                        downloadListener.onCancelled(id, DownloadHelper.calculateProgress(requestData.getDownloadedBytes(), requestData.getTotalBytes())
                                , requestData.getDownloadedBytes(), requestData.getTotalBytes());
                    }
                }
            }

            @Override
            public void onPostExecute() {

            }
        });
        actionProcessor.processNext();
    }

    void cancelAll() {
        if (isDisposed) {
            return;
        }

        databaseManager.executeTransaction(new Transaction() {

            @Override
            public void onPreExecute() {

            }

            @Override
            public void onExecute(Database database) {
                List<RequestData> list = database.query();

                for (RequestData requestData : list) {

                    if(DownloadHelper.canCancel(requestData.getStatus())) {

                        if(downloadsMap.containsKey(requestData.getId())){
                            interrupt(requestData.getId(), InterruptReason.CANCELLED);

                        }else {

                            database.setStatusAndError(requestData.getId(), Status.CANCELLED, Error.NONE.getValue());
                            downloadListener.onCancelled(requestData.getId(),
                                    DownloadHelper.calculateProgress(requestData.getDownloadedBytes(), requestData.getTotalBytes())
                                    , requestData.getDownloadedBytes(), requestData.getTotalBytes());
                        }
                    }
                }
            }

            @Override
            public void onPostExecute() {

            }
        });
        actionProcessor.processNext();
    }

    void remove(final long id) {
        if (isDisposed) {
            return;
        }

        if(downloadsMap.containsKey(id)) {
            interrupt(id, InterruptReason.REMOVED);
        }else {

            databaseManager.executeTransaction(new Transaction() {

                @Override
                public void onPreExecute() {

                }

                @Override
                public void onExecute(Database database) {
                    RequestData requestData = database.query(id);
                    if(requestData != null) {
                        database.remove(id);

                        downloadListener.onRemoved(id,
                                DownloadHelper.calculateProgress(requestData.getDownloadedBytes(), requestData.getTotalBytes())
                                , requestData.getDownloadedBytes(), requestData.getTotalBytes());
                    }
                }

                @Override
                public void onPostExecute() {

                }
            });
        }

        actionProcessor.processNext();
    }

    void removeAll() {
        if (isDisposed) {
            return;
        }

        databaseManager.executeTransaction(new Transaction() {

            @Override
            public void onPreExecute() {

            }

            @Override
            public void onExecute(Database database) {
                List<RequestData> list = database.query();

                for (RequestData requestData : list) {

                    if(downloadsMap.containsKey(requestData.getId())){
                        interrupt(requestData.getId(), InterruptReason.REMOVED);

                    }else {
                        database.remove(requestData.getId());
                        downloadListener.onRemoved(requestData.getId(),
                                DownloadHelper.calculateProgress(requestData.getDownloadedBytes(), requestData.getTotalBytes())
                                , requestData.getDownloadedBytes(), requestData.getTotalBytes());
                    }
                }
            }

            @Override
            public void onPostExecute() {

            }
        });

        actionProcessor.processNext();
    }

    private void interrupt(long id,InterruptReason interruptReason) {
        if(downloadsMap.containsKey(id)) {
            DownloadRunnable downloadRunnable = downloadsMap.get(id);

            if (downloadRunnable != null) {
                downloadRunnable.interrupt(interruptReason);

                try {
                    downloadRunnable.getThread().join(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void interruptAll(InterruptReason reason) {
        Set<Long> keys = downloadsMap.keySet();

        for (Long key : keys) {

            DownloadRunnable downloadRunnable = downloadsMap.get(key);

            if (downloadRunnable != null) {
                downloadRunnable.interrupt(reason);

                try {
                    downloadRunnable.getThread().join(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void download(final RequestData requestData) {
        if(requestData == null || downloadsMap.containsKey(requestData.getId())) {
            return;
        }

        DownloadRunnable downloadRunnable = new DownloadRunnable(requestData);
        Thread thread = new Thread(downloadRunnable);
        downloadRunnable.setThread(thread);
        downloadsMap.put(requestData.getId(),downloadRunnable);
        thread.start();
    }

    private class DownloadRunnable implements Runnable {
        private final RequestData request;
        private volatile boolean isInterrupted;
        private InterruptReason interruptReason;
        private Thread thread;

        Response response = null;
        ResponseBody body = null;
        BufferedInputStream input = null;
        RandomAccessFile output = null;
        long downloadedBytes = 0L;
        long totalBytes = 0L;
        int progress = 0;



        DownloadRunnable(RequestData request) {
            this.request = request;
            this.isInterrupted = false;
        }

        void setThread(Thread thread) {
            this.thread = thread;
        }

        void interrupt(InterruptReason reason){
            if(isInterrupted){
                return;
            }
            interruptReason = reason;
            isInterrupted = true;
        }

        Thread getThread() {
            return thread;
        }

        boolean isInterrupted() {
            return isInterrupted;
        }

        @Override
        public void run() {

            String oldThreadName = thread.getName();
            thread.setName("DownloaderThread url:"+request.getUrl());

            try {

                File file = DownloadHelper.createFileOrThrow(request.getAbsoluteFilePath());
                downloadedBytes = file.length();
                totalBytes = request.getTotalBytes();
                progress = DownloadHelper.calculateProgress(totalBytes, downloadedBytes);

                if (!isInterrupted()) {

                    Call call = okHttpClient.newCall(DownloadHelper.createHttpRequest(request));
                    response = call.execute();
                    body = response.body();

                    if(response.isSuccessful() && body != null && !isInterrupted()) {

                        totalBytes = downloadedBytes + Long.valueOf(response.header("Content-Length"));

                        databaseManager.executeTransaction(new Transaction() {

                            @Override
                            public void onPreExecute() {

                            }

                            @Override
                            public void onExecute(Database database) {
                                database.setDownloadedBytesAndTotalBytes(request.getId(),downloadedBytes,totalBytes);
                            }

                            @Override
                            public void onPostExecute() {

                            }
                        });

                        input = new BufferedInputStream(body.byteStream());
                        output = new RandomAccessFile(request.getAbsoluteFilePath(), "rw");

                        if (response.code() == HttpURLConnection.HTTP_PARTIAL) {
                            output.seek(downloadedBytes);
                        } else {
                            output.seek(0);
                        }

                        byte[] buffer = new byte[1024];
                        int read;
                        long startTime, stopTime;

                        startTime = System.nanoTime();
                        while((read = input.read(buffer, 0, 1024)) != -1 && !isInterrupted()) {
                            output.write(buffer, 0, read);
                            downloadedBytes += read;

                            databaseManager.executeTransaction(new Transaction() {

                                @Override
                                public void onPreExecute() {

                                }

                                @Override
                                public void onExecute(Database database) {
                                    database.updateDownloadedBytes(request.getId(), downloadedBytes);
                                }

                                @Override
                                public void onPostExecute() {

                                }
                            });

                            progress = DownloadHelper.calculateProgress(downloadedBytes,totalBytes);

                            stopTime = System.nanoTime();
                            if (DownloadHelper.hasTwoSecondsPassed(startTime, stopTime)) {
                                downloadListener.onProgress(request.getId(), progress, downloadedBytes, totalBytes);
                                startTime = System.nanoTime();
                            }
                        }
                    }else if(!response.isSuccessful()) {
                        throw new IOException("invalid server response");
                    }
                }

                databaseManager.executeTransaction(new Transaction() {

                    @Override
                    public void onPreExecute() {

                    }

                    @Override
                    public void onExecute(Database database) {
                        database.setDownloadedBytesAndTotalBytes(request.getId(), downloadedBytes, totalBytes);
                    }

                    @Override
                    public void onPostExecute() {

                    }
                });

                progress = DownloadHelper.calculateProgress(downloadedBytes, totalBytes);
                downloadListener.onProgress(request.getId(), progress, downloadedBytes, totalBytes);

                if (!isInterrupted()) {

                    databaseManager.executeTransaction(new Transaction() {

                        @Override
                        public void onPreExecute() {

                        }

                        @Override
                        public void onExecute(Database database) {
                            database.setStatusAndError(request.getId(), Status.COMPLETED, Error.NONE.getValue());
                        }

                        @Override
                        public void onPostExecute() {

                        }
                    });

                    downloadListener.onComplete(request.getId(), progress, downloadedBytes, totalBytes);
                } else {
                    switch (interruptReason) {
                        case PAUSED: {

                            databaseManager.executeTransaction(new Transaction() {

                                @Override
                                public void onPreExecute() {

                                }

                                @Override
                                public void onExecute(Database database) {
                                    database.setStatusAndError(request.getId(), Status.PAUSED, Error.NONE.getValue());
                                }

                                @Override
                                public void onPostExecute() {

                                }
                            });

                            downloadListener.onPause(request.getId(), progress, downloadedBytes, totalBytes);
                            break;
                        }
                        case CANCELLED: {

                            databaseManager.executeTransaction(new Transaction() {

                                @Override
                                public void onPreExecute() {

                                }

                                @Override
                                public void onExecute(Database database) {
                                    database.setStatusAndError(request.getId(), Status.CANCELLED, Error.NONE.getValue());
                                }

                                @Override
                                public void onPostExecute() {

                                }
                            });

                            downloadListener.onCancelled(request.getId(), progress, downloadedBytes, totalBytes);
                            break;
                        }
                        case REMOVED: {

                            databaseManager.executeTransaction(new Transaction() {

                                @Override
                                public void onPreExecute() {

                                }

                                @Override
                                public void onExecute(Database database) {
                                    database.remove(request.getId());
                                }

                                @Override
                                public void onPostExecute() {

                                }
                            });

                            downloadListener.onRemoved(request.getId(), progress, downloadedBytes, totalBytes);
                            break;
                        }
                    }
                }
            }catch (Exception e){
                final Error reason = ErrorUtils.getCode(e.getMessage());

                if(!NetworkUtils.isNetworkAvailable(context) && reason == Error.HTTP_NOT_FOUND) {

                    databaseManager.executeTransaction(new Transaction() {

                        @Override
                        public void onPreExecute() {

                        }

                        @Override
                        public void onExecute(Database database) {
                            database.setStatusAndError(request.getId(), Status.ERROR, Error.NO_NETWORK_CONNECTION.getValue());
                        }

                        @Override
                        public void onPostExecute() {

                        }
                    });

                }else{
                    databaseManager.executeTransaction(new Transaction() {

                        @Override
                        public void onPreExecute() {

                        }

                        @Override
                        public void onExecute(Database database) {
                            database.setStatusAndError(request.getId(), Status.ERROR,reason.getValue());
                        }

                        @Override
                        public void onPostExecute() {

                        }
                    });
                }

                downloadListener.onError(request.getId(),reason,progress,downloadedBytes,totalBytes);
            }finally {
                downloadsMap.remove(request.getId());

                if(response != null) {
                    response.close();
                }
                if(body != null) {
                    body.close();
                }
                if(output != null){
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                thread.setName(oldThreadName);
            }
        }
    }

    enum InterruptReason {
        PAUSED,
        CANCELLED,
        REMOVED
    }

    @Override
    public synchronized void dispose() {
        if(!isDisposed) {
            pauseAll();
            isDisposed = true;
        }
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
}
