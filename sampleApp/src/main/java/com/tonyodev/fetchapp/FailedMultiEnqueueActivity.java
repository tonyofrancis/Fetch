package com.tonyodev.fetchapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FailedMultiEnqueueActivity extends AppCompatActivity implements FetchListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_enqueue);
        final View mainView = findViewById(R.id.activity_main);
        final Fetch fetch = ((App) getApplication()).getFetch();
        fetch.deleteAll();
        final List<Request> requests = new ArrayList<>();
        final String url = "https://www.notdownloadable.com/test.txt";
        final int size = 15;

        for (int x = 0; x < size; x++) {

            final String filePath = Data.getSaveDir()
                    .concat("/multiTest/")
                    .concat("file")
                    .concat("" + (x + 1))
                    .concat(".txt");

            final Request request = new Request(url, filePath);
            requests.add(request);
        }

        fetch.enqueue(requests, null, null);

        Snackbar.make(mainView, "Enqueued " + size + " requests. Check Logcat for " +
                "failed status", Snackbar.LENGTH_INDEFINITE)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((App) getApplication()).getFetch().addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ((App) getApplication()).getFetch().removeListener(this);
    }

    @Override
    public void onQueued(@NotNull Download download) {

    }

    @Override
    public void onCompleted(@NotNull Download download) {

    }

    @Override
    public void onError(@NotNull Download download) {

    }

    @Override
    public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {

    }

    @Override
    public void onPaused(@NotNull Download download) {

    }

    @Override
    public void onResumed(@NotNull Download download) {

    }

    @Override
    public void onCancelled(@NotNull Download download) {

    }

    @Override
    public void onRemoved(@NotNull Download download) {

    }

    @Override
    public void onDeleted(@NotNull Download download) {

    }
}
