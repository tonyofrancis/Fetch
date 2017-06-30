package com.tonyodev.fetch2.download;

import android.content.Context;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.RequestData;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2.database.Database;
import com.tonyodev.fetch2.database.DatabaseException;
import com.tonyodev.fetch2.database.DatabaseManager;
import com.tonyodev.fetch2.database.Transaction;
import com.tonyodev.fetch2.util.ErrorUtils;
import com.tonyodev.fetch2.util.NetworkUtils;
import com.tonyodev.fetch2.util.Utils;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by tonyofrancis on 6/24/17.
 */

class DownloadRunnable implements Runnable, Closeable {

    private final long requestId;
    private final OkHttpClient okHttpClient;
    private final DatabaseManager databaseManager;
    private final DownloadListener downloadListener;
    private final Context context;

    private String url;
    private String filePath;
    private Map<String,String> headers;
    private Response response = null;
    private ResponseBody body = null;
    private BufferedInputStream input = null;
    private RandomAccessFile output = null;
    private long downloadedBytes = 0L;
    private long totalBytes = 0L;
    private int progress = 0;
    private volatile boolean isInterrupted;

    DownloadRunnable(long requestId, OkHttpClient okHttpClient, DatabaseManager databaseManager, DownloadListener downloadListener,Context context) {
        this.requestId = requestId;
        this.isInterrupted = false;
        this.okHttpClient = okHttpClient;
        this.databaseManager = databaseManager;
        this.downloadListener = downloadListener;
        this.context = context;
    }

    synchronized void interrupt() {
        if(isInterrupted) {
            return;
        }
        isInterrupted = true;
    }

    boolean isInterrupted() {
        return isInterrupted;
    }

    @Override
    public void run() {
        //Rename thread for debugging
        final Thread thread = Thread.currentThread();
        final String oldThreadName = thread.getName();
        thread.setName("DownloadRunnable -"+ url);

        try {
            init();
            if (!isInterrupted) {
                executeRequest();
                if(response.isSuccessful() && body != null && !isInterrupted) {
                    setTotalBytesFromContentLength();
                    updateDatabase();
                    updateDatabaseWithDownloadingStatus();
                    writeToFile();
                }else if(!response.isSuccessful() || body == null) {
                    throw new IOException("invalid server response");
                }
            }
            updateDatabase();
            postProgress();

            if (!isInterrupted) {
                updateDatabaseWithCompleteStatus();
                postComplete();
            }
        }catch (Exception e){
            Error reason = ErrorUtils.getCode(e.getMessage());
            handleException(reason);
        }finally {
            try {
                close();
            }catch (IOException e) {
                e.printStackTrace();
            }
            thread.setName(oldThreadName);
        }
    }

    private void init() throws IOException, DatabaseException {
        databaseManager.executeTransaction(new Transaction() {
            @Override
            public void onExecute(Database database) {
                RequestData requestData = database.query(requestId);
                if (requestData == null) {
                    throw new DatabaseException("Request not found in the database");
                }

                url = requestData.getUrl();
                filePath = requestData.getAbsoluteFilePath();
                totalBytes = requestData.getTotalBytes();
                headers = requestData.getHeaders();
            }
        });
        File file = Utils.createFileOrThrow(filePath);
        downloadedBytes = file.length();
        progress = Utils.calculateProgress(downloadedBytes, totalBytes);
    }

    private void updateDatabase() {
        databaseManager.executeTransaction(new Transaction() {
            @Override
            public void onExecute(Database database) {
                database.setDownloadedBytesAndTotalBytes(requestId, downloadedBytes, totalBytes);
            }
        });
    }

    private void updateDatabaseWithCompleteStatus() {
        databaseManager.executeTransaction(new Transaction() {
            @Override
            public void onExecute(Database database) {
                database.setStatusAndError(requestId, Status.COMPLETED.getValue(),Error.NONE.getValue());
            }
        });
    }

    private void updateDatabaseWithDownloadingStatus() {
        databaseManager.executeTransaction(new Transaction() {
            @Override
            public void onExecute(Database database) {
                database.setStatusAndError(requestId,Status.DOWNLOADING.getValue(),Error.NONE.getValue());
            }
        });
    }

    private void postProgress() {
        progress = Utils.calculateProgress(downloadedBytes, totalBytes);
        downloadListener.onProgress(requestId, progress, downloadedBytes, totalBytes);
    }

    private void postComplete() {
        downloadListener.onComplete(requestId, progress, downloadedBytes, totalBytes);
    }

    private void executeRequest() throws IOException {
        final Call call = okHttpClient.newCall(getHttpRequest());
        response = call.execute();
        body = response.body();
    }

    private void setTotalBytesFromContentLength() {
        totalBytes = downloadedBytes + Long.valueOf(response.header("Content-Length"));
    }

    private okhttp3.Request getHttpRequest() {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(url);

        for (String key : headers.keySet()) {
            builder.addHeader(key,headers.get(key));
        }

        builder.addHeader("Range","bytes=" + downloadedBytes + "-");
        return builder.build();
    }

    private boolean hasTwoSecondsPassed(long startTime, long stopTime) {
        return TimeUnit.NANOSECONDS.toSeconds(stopTime - startTime) >= 2;
    }

    private void writeToFile() throws IOException {
        input = new BufferedInputStream(body.byteStream());
        output = new RandomAccessFile(filePath, "rw");

        if (response.code() == HttpURLConnection.HTTP_PARTIAL) {
            output.seek(downloadedBytes);
        } else {
            output.seek(0);
        }

        byte[] buffer = new byte[1024];
        int read;
        long startTime;
        long stopTime;

        startTime = System.nanoTime();
        while ((read = input.read(buffer, 0, 1024)) != -1 && !isInterrupted) {
            output.write(buffer, 0, read);
            downloadedBytes += read;

            stopTime = System.nanoTime();
            if (hasTwoSecondsPassed(startTime, stopTime)) {
                postProgress();
                startTime = System.nanoTime();
            }
        }
    }

    @Override
    public void close() throws IOException {
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
    }

    private void handleException(final Error reason) {
        if(!NetworkUtils.isNetworkAvailable(context) && reason == Error.HTTP_NOT_FOUND) {
            databaseManager.executeTransaction(new Transaction() {
                @Override
                public void onExecute(Database database) {
                    database.setStatusAndError(requestId, Status.ERROR.getValue(), Error.NO_NETWORK_CONNECTION.getValue());
                }
            });
            downloadListener.onError(requestId,Error.NO_NETWORK_CONNECTION,progress,downloadedBytes,totalBytes);
        }else{
            databaseManager.executeTransaction(new Transaction() {
                @Override
                public void onExecute(Database database) {
                    database.setStatusAndError(requestId, Status.ERROR.getValue(), reason.getValue());
                }
            });
            downloadListener.onError(requestId,reason,progress,downloadedBytes,totalBytes);
        }
    }
}