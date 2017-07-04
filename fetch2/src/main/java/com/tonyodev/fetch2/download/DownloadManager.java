package com.tonyodev.fetch2.download;

import android.content.Context;
import android.support.v4.util.Pair;

import com.tonyodev.fetch2.database.Database;

import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;

public final class DownloadManager implements Downloadable {

    private final Context context;
    private final OkHttpClient okHttpClient;
    private final Database database;
    private final DownloadListener downloadListener;
    private final ConcurrentHashMap<Long,Pair<Thread,Runnable>> downloadsMap;

    public DownloadManager(Context context, OkHttpClient client, DownloadListener downloadListener, Database database) {
        this.context = context;
        this.okHttpClient = client;
        this.database = database;
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
    public void remove(long id) {
        interrupt(id);
    }

    private synchronized void interrupt(long id) {
        if(downloadsMap.containsKey(id)) {
            Pair<Thread,Runnable> threadRunnablePair = downloadsMap.get(id);

            if (threadRunnablePair != null) {
                ((DownloadRunnable)threadRunnablePair.second).interrupt();
                try {
                    threadRunnablePair.first.join(100);
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
        Runnable downloadRunnable = new DownloadRunnable(id,context,okHttpClient,database,downloadListener);
        Thread thread = new Thread(downloadRunnable);
        Pair<Thread,Runnable> threadRunnablePair = Pair.create(thread,downloadRunnable);
        downloadsMap.put(id, threadRunnablePair);
        thread.start();
    }
}