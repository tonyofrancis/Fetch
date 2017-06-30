package com.tonyodev.fetch2.download;

import android.content.Context;

import com.tonyodev.fetch2.database.DatabaseManager;

import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;

public final class DownloadManager implements Downloadable {

    private final Context context;
    private final OkHttpClient okHttpClient;
    private final DatabaseManager databaseManager;
    private final DownloadListener downloadListener;
    private final ConcurrentHashMap<Long,ThreadRunnablePair> downloadsMap;

    public DownloadManager(Context context, OkHttpClient client, DownloadListener downloadListener, DatabaseManager databaseManager ) {
        this.context = context;
        this.okHttpClient = client;
        this.databaseManager = databaseManager;
        this.downloadListener = downloadListener;
        this.downloadsMap = new ConcurrentHashMap<>();
    }

    @Override
    public void pause(long id) {
        interrupt(id);
    }

    @Override
    public void resume(long id) {
        download(id);
    }

    @Override
    public void retry(long id) {
        resume(id);
    }

    @Override
    public void cancel(long id) {
        interrupt(id);
    }

    @Override
    public void remove(long id) {
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
}