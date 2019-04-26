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
import com.tonyodev.fetch2core.FetchObserver;
import com.tonyodev.fetch2core.Reason;

import org.jetbrains.annotations.NotNull;

public class ProgressFragment extends Fragment implements FetchObserver<Download> {

    private ProgressBar progressBar;
    private TextView progressTextView;

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

    @Override
    public void onChanged(Download data, @NotNull Reason reason) {
        updateProgress(data.getProgress());
    }

    private void updateProgress(int progress) {
        if (progress == -1) {
            progress = 0;
        }
        progressBar.setProgress(progress);
        progressTextView.setText(getResources().getString(R.string.percent_progress, progress));
    }

}