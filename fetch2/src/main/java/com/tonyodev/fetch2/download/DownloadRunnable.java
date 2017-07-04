package com.tonyodev.fetch2.download;

import android.content.Context;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2.database.Database;
import com.tonyodev.fetch2.database.DatabaseException;
import com.tonyodev.fetch2.database.DatabaseRow;
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

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by tonyofrancis on 6/24/17.
 */

public final class DownloadRunnable implements Runnable, Closeable {

    private final long requestId;
    private final OkHttpClient okHttpClient;
    private final Database database;
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
    private volatile boolean isInterrupted = false;

    public DownloadRunnable(long requestId, Context context, OkHttpClient okHttpClient, Database database, DownloadListener downloadListener) {
        this.requestId = requestId;
        this.context = context;
        this.okHttpClient = okHttpClient;
        this.database = database;
        this.downloadListener = downloadListener;
    }

    public void interrupt() {
        isInterrupted = true;
    }

    public boolean isInterrupted() {
        return isInterrupted;
    }

    @Override
    public void run() {
        //Rename thread for debugging
        final Thread thread = Thread.currentThread();
        final String oldThreadName = thread.getName();

        try {
            init();
            thread.setName("DownloadRunnable -"+ url);
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
        DatabaseRow row = database.query(requestId);
        if (row == null) {
            throw new DatabaseException("Request not found in the database");
        }
        url = row.getUrl();
        filePath = row.getAbsoluteFilePath();
        totalBytes = row.getTotalBytes();
        headers = row.getHeaders();
        File file = Utils.createFileOrThrow(filePath);
        downloadedBytes = file.length();
        progress = Utils.calculateProgress(downloadedBytes, totalBytes);
    }

    private void updateDatabase() {
        database.setDownloadedBytesAndTotalBytes(requestId, downloadedBytes, totalBytes);
    }

    private void updateDatabaseWithCompleteStatus() {
        database.setStatusAndError(requestId, Status.COMPLETED.getValue(),Error.NONE.getValue());
    }

    private void updateDatabaseWithDownloadingStatus() {
        database.setStatusAndError(requestId,Status.DOWNLOADING.getValue(),Error.NONE.getValue());
    }

    private void updateDatabaseBaseWithDownloadedBytes() {
        database.updateDownloadedBytes(requestId, downloadedBytes);
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

    private okhttp3.Request getHttpRequest() {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(url);

        for (String key : headers.keySet()) {
            builder.addHeader(key,headers.get(key));
        }

        builder.addHeader("Range","bytes=" + downloadedBytes + "-");
        return builder.build();
    }

    private void setTotalBytesFromContentLength() {
        totalBytes = downloadedBytes + Long.valueOf(response.header("Content-Length"));
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

        while ((read = input.read(buffer, 0, 1024)) != -1 && !isInterrupted) {
            output.write(buffer, 0, read);
            downloadedBytes += read;
            updateDatabaseBaseWithDownloadedBytes();
            postProgress();
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
            database.setStatusAndError(requestId, Status.ERROR.getValue(), Error.NO_NETWORK_CONNECTION.getValue());
            downloadListener.onError(requestId,Error.NO_NETWORK_CONNECTION,progress,downloadedBytes,totalBytes);
        }else{
            database.setStatusAndError(requestId, Status.ERROR.getValue(), reason.getValue());
            downloadListener.onError(requestId,reason,progress,downloadedBytes,totalBytes);
        }
    }
}