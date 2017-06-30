package com.tonyodev.fetch2.download;

import android.content.Context;

import com.tonyodev.fetch2.core.Disposable;
import com.tonyodev.fetch2.core.DisposedException;
import com.tonyodev.fetch2.database.DatabaseManager;
import com.tonyodev.fetch2.listener.DownloadListener;

import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;

public final class DownloadManager implements Downloadable, Disposable {

    private final Context context;
    private final OkHttpClient okHttpClient;
    private final DatabaseManager databaseManager;
    private final DownloadListener downloadListener;
    private final ConcurrentHashMap<Long,ThreadRunnablePair> downloadsMap;
    private volatile boolean isDisposed;

    public DownloadManager(Context context, OkHttpClient client, DownloadListener downloadListener, DatabaseManager databaseManager ) {
        this.isDisposed = false;
        this.context = context;
        this.okHttpClient = client;
        this.databaseManager = databaseManager;
        this.downloadListener = downloadListener;
        this.downloadsMap = new ConcurrentHashMap<>();
    }

    @Override
    public void pause(long id) {
        throwIfDisposed();
        interrupt(id);
    }

    @Override
    public void resume(long id) {
        throwIfDisposed();
        download(id);
    }

    @Override
    public void retry(long id) {
        throwIfDisposed();
        resume(id);
    }

    @Override
    public void cancel(long id) {
        throwIfDisposed();
        interrupt(id);
    }

    @Override
    public void remove(long id) {
        throwIfDisposed();
        interrupt(id);
    }

    private void interrupt(long id) {
        if(downloadsMap.containsKey(id)) {
            ThreadRunnablePair threadRunnablePair = downloadsMap.get(id);

            if (threadRunnablePair != null) {
                threadRunnablePair.downloadRunnable.interrupt();

                try {
                    threadRunnablePair.thread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    downloadsMap.remove(id);
                }
            }
        }
    }

    private void interrupt() {
        for (Long id : downloadsMap.keySet()) {
            interrupt(id);
        }
    }

    private synchronized void download(long id) {
        if(downloadsMap.containsKey(id)) {
            return;
        }

        DownloadRunnable downloadRunnable = new DownloadRunnable(id,okHttpClient,databaseManager,downloadListener,context);
        Thread thread = new Thread(downloadRunnable);
        ThreadRunnablePair ThreadRunnablePair = new ThreadRunnablePair(thread, downloadRunnable);
        downloadsMap.put(id, ThreadRunnablePair);
        thread.start();
    }

    @Override
    public synchronized void dispose() {
        if(!isDisposed) {
            interrupt();
            isDisposed = true;
        }
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }

    private void throwIfDisposed() {
        if (isDisposed) {
            throw new DisposedException("DownloadManager has already been disposed");
        }
    }
}