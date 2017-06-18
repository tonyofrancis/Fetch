package com.tonyodev.fetch2;


import android.support.annotation.NonNull;

interface DownloadListener {
    void onComplete(long id, int progress, long downloadedBytes, long totalBytes);
    void onError(long id, @NonNull Error error, int progress, long downloadedBytes, long totalBytes);
    void onProgress(long id, int progress, long downloadedBytes, long totalBytes);
    void onPause(long id, int progress, long downloadedBytes, long totalBytes);
    void onCancelled(long id, int progress, long downloadedBytes, long totalBytes);
    void onRemoved(long id, int progress, long downloadedBytes, long totalBytes);
}
