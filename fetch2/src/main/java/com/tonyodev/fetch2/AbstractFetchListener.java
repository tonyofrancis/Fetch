package com.tonyodev.fetch2;

import android.support.annotation.NonNull;

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
    public void onError(long id,@NonNull Error error, int progress, long downloadedBytes, long totalBytes) {

    }

    @Override
    public void onProgress(long id, int progress, long downloadedBytes, long totalBytes) {

    }

    @Override
    public void onPause(long id, int progress, long downloadedBytes, long totalBytes) {

    }

    @Override
    public void onCancelled(long id, int progress, long downloadedBytes, long totalBytes) {

    }

    @Override
    public void onRemoved(long id, int progress, long downloadedBytes, long totalBytes) {

    }
}
