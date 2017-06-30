package com.tonyodev.fetch2.download;

/**
 * Created by tonyofrancis on 6/29/17.
 */

public final class ThreadRunnablePair {
    public final Thread thread;
    public final DownloadRunnable downloadRunnable;

    public ThreadRunnablePair(Thread thread, DownloadRunnable downloadRunnable) {
        this.thread = thread;
        this.downloadRunnable = downloadRunnable;
    }
}