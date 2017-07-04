package com.tonyodev.fetch2.listener;

import android.support.annotation.NonNull;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;

public abstract class AbstractFetchListener implements FetchListener {

    @Override
    public void onAttach(@NonNull Fetch fetch) {

    }

    @Override
    public void onDetach(@NonNull Fetch fetch) {

    }

    @Override
    public void onComplete(long id, int progress, long downloadedBytes, long totalBytes) {

    }

    @Override
    public void onError(long id, @NonNull Error error, int progress, long downloadedBytes, long totalBytes) {

    }

    @Override
    public void onProgress(long id, int progress, long downloadedBytes, long totalBytes) {

    }

    @Override
    public void onPaused(long id, int progress, long downloadedBytes, long totalBytes) {

    }

    @Override
    public void onCancelled(long id, int progress, long downloadedBytes, long totalBytes) {

    }

    @Override
    public void onRemoved(long id, int progress, long downloadedBytes, long totalBytes) {

    }
}
