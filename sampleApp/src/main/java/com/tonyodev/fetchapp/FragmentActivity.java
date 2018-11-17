package com.tonyodev.fetchapp;

import android.Manifest;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class FragmentActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 150;

    private View rootView;
    private ProgressFragment progressFragment1;
    private ProgressFragment progressFragment2;

    private Fetch fetch;
    private Request request;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_progress);
        rootView = findViewById(R.id.rootView);
        fetch = Fetch.Impl.getDefaultInstance();
        final FragmentManager fragmentManager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            progressFragment1 = new ProgressFragment();
            progressFragment2 = new ProgressFragment();
            fragmentManager.beginTransaction().add(R.id.fragment1, progressFragment1).add(R.id.fragment2, progressFragment2).commit();
        } else {
            progressFragment1 = (ProgressFragment) fragmentManager.findFragmentById(R.id.fragment1);
            progressFragment2 = (ProgressFragment) fragmentManager.findFragmentById(R.id.fragment2);
        }
        checkStoragePermissions();
    }

    private void setRequestForFragments(@NotNull final Download download) {
        if (progressFragment1 != null) {
            progressFragment1.setRequest(request);
            progressFragment1.updateProgress(download.getProgress());
        }
        if (progressFragment2 != null) {
            progressFragment2.setRequest(request);
            progressFragment2.updateProgress(download.getProgress());
        }
    }

    private void checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else {
            enqueueDownload();
        }
    }

    private void enqueueDownload() {
        final String url = Data.sampleUrls[0];
        final String filePath = Data.getSaveDir() + "/fragments/movie.mp4";
        request = new Request(url, filePath);
        fetch.enqueue(request, updatedRequest -> {
            request = updatedRequest;
        }, error -> {
            Timber.d("FragmentActivity Error: %1$s", error.toString());
            Snackbar.make(rootView, R.string.enqueue_error, Snackbar.LENGTH_INDEFINITE).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetch.addListener(progressFragment1)
                .addListener(progressFragment2)
                .addListener(fetchListener);
        if (request != null) {
            fetch.getDownload(request.getId(), download -> {
                if (download != null) {
                    setRequestForFragments(download);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeListener(progressFragment1)
                .removeListener(progressFragment2)
                .removeListener(fetchListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetch.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueDownload();
        } else {
            Snackbar.make(rootView, R.string.permission_not_enabled, Snackbar.LENGTH_LONG).show();
        }
    }

    private final FetchListener fetchListener = new AbstractFetchListener() {

        @Override
        public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
            super.onQueued(download, waitingOnNetwork);
            setRequestForFragments(download);
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
            if (request != null && request.getId() == download.getId()) {
                Timber.d("FragmentActivity id: %1$d, status: %2$s, progress: %3$d, error: %4$s", download.getId(), download.getStatus(), download.getProgress(), download.getError());
            }
        }

    };
}