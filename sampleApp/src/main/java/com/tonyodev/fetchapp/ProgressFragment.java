package com.tonyodev.fetchapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.DownloadBlock;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProgressFragment extends Fragment implements FetchListener {

    private ProgressBar progressBar;
    private TextView progressTextView;
    private Request request;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = view.findViewById(R.id.progressBar);
        progressTextView = view.findViewById(R.id.progressTextView);
        updateProgress(0);
    }

    public void updateProgress(int progress) {
        if (progress == -1) {
            progress = 0;
        }
        progressBar.setProgress(progress);
        progressTextView.setText(getResources().getString(R.string.percent_progress, progress));
    }

    public int getDownloadId() {
        if (request != null) {
            return request.getId();
        }
        return -1;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    private void updateProgressForDownload(@NotNull Download download) {
        if (download.getId() == getDownloadId()) {
            updateProgress(download.getProgress());
        }
    }

    @Override
    public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
        updateProgressForDownload(download);
    }

    @Override
    public void onCompleted(@NotNull Download download) {
        updateProgressForDownload(download);
    }

    @Override
    public void onError(@NotNull Download download, @NotNull Error error, @org.jetbrains.annotations.Nullable Throwable throwable) {
        updateProgressForDownload(download);
    }

    @Override
    public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int totalBlocks) {

    }

    @Override
    public void onStarted(@NotNull Download download, @NotNull List<? extends DownloadBlock> downloadBlocks, int totalBlocks) {
        updateProgressForDownload(download);
    }

    @Override
    public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
        updateProgressForDownload(download);
    }

    @Override
    public void onPaused(@NotNull Download download) {
        updateProgressForDownload(download);
    }

    @Override
    public void onResumed(@NotNull Download download) {
        updateProgressForDownload(download);
    }

    @Override
    public void onCancelled(@NotNull Download download) {
        updateProgressForDownload(download);
    }

    @Override
    public void onRemoved(@NotNull Download download) {
        updateProgressForDownload(download);
    }

    @Override
    public void onDeleted(@NotNull Download download) {
        updateProgressForDownload(download);
    }

    @Override
    public void onAdded(@NotNull Download download) {
        updateProgressForDownload(download);
    }

    @Override
    public void onWaitingNetwork(@NotNull Download download) {
        //Called on background thread
    }
}