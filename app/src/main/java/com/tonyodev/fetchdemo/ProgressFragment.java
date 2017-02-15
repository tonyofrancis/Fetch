package com.tonyodev.fetchdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.listener.FetchListener;

/**
 * Created by tonyofrancis on 1/31/17.
 */

public class ProgressFragment extends Fragment implements FetchListener {

    private long downloadId = Fetch.DEFAULT_EMPTY_VALUE;

    private ProgressBar progressBar;
    private TextView progressTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_progress,container,false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressTextView = (TextView) view.findViewById(R.id.progressTextView);
        updateProgress(0);
    }

    private void updateProgress(int progress) {

        progressBar.setProgress(progress);
        progressTextView.setText(getResources().getString(R.string.percent_progress,progress));
    }

    @Override
    public void onUpdate(long id, int status, int progress, long writtenBytes, long fileSize, int error) {

        if (id == getDownloadId()) {

            updateProgress(progress);
        }
    }

    public long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }
}