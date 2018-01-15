package com.tonyodev.fetchapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;

public class ProgressFragment extends Fragment implements FetchListener {

    private ProgressBar progressBar;
    private TextView progressTextView;
    private Request request;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressTextView = (TextView) view.findViewById(R.id.progressTextView);
        updateProgress(0);
    }

    private void updateProgress(int progress) {
        if (progress == -1) {
            progress = 0;
        }
        progressBar.setProgress(progress);
        progressTextView.setText(getResources().getString(R.string.percent_progress, progress));
    }

    public int getDownloadId() {
        return request.getId();
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    @Override
    public void onQueued(@NotNull Download download) {
        if (download.getId() == getDownloadId()) {
            updateProgress(download.getProgress());
        }
    }

    @Override
    public void onCompleted(@NotNull Download download) {
        if (download.getId() == getDownloadId()) {
            updateProgress(download.getProgress());
        }
    }

    @Override
    public void onError(@NotNull Download download) {
        if (download.getId() == getDownloadId()) {
            updateProgress(download.getProgress());
        }
    }

    @Override
    public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
        if (download.getId() == getDownloadId()) {
            updateProgress(download.getProgress());
        }
    }

    @Override
    public void onPaused(@NotNull Download download) {
        if (download.getId() == getDownloadId()) {
            updateProgress(download.getProgress());
        }
    }

    @Override
    public void onResumed(@NotNull Download download) {
        if (download.getId() == getDownloadId()) {
            updateProgress(download.getProgress());
        }
    }

    @Override
    public void onCancelled(@NotNull Download download) {
        if (download.getId() == getDownloadId()) {
            updateProgress(download.getProgress());
        }
    }

    @Override
    public void onRemoved(@NotNull Download download) {
        if (download.getId() == getDownloadId()) {
            updateProgress(download.getProgress());
        }
    }

    @Override
    public void onDeleted(@NotNull Download download) {
        if (download.getId() == getDownloadId()) {
            updateProgress(download.getProgress());
        }
    }

}