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
import android.util.Log;
import android.view.View;

import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Func;
import com.tonyodev.fetch2.Request;

import org.jetbrains.annotations.NotNull;

public class FragmentActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 150;

    private Fetch fetch;

    private View rootView;
    private ProgressFragment progressFragment1;
    private ProgressFragment progressFragment2;

    private Request request;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_progress);
        rootView = findViewById(R.id.rootView);
        fetch = ((App) getApplication()).getFetch();

        if (savedInstanceState == null) {
            fetch.deleteAll();
            progressFragment1 = new ProgressFragment();
            progressFragment2 = new ProgressFragment();

            final FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .add(R.id.fragment1, progressFragment1)
                    .add(R.id.fragment2, progressFragment2)
                    .commit();
            checkStoragePermissions();
        }
    }

    private void checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    , STORAGE_PERMISSION_CODE);
        } else {
            enqueueDownload();
        }
    }

    private void enqueueDownload() {
        final String url = Data.sampleUrls[0];
        final String filePath = Data.getSaveDir() + "/fragments/smallFile.txt";

        request = new Request(url, filePath);
        fetch.enqueue(request, new Func<Download>() {
            @Override
            public void call(Download download) {
                progressFragment1.setRequest(request);
                progressFragment2.setRequest(request);
            }
        }, new Func<Error>() {
            @Override
            public void call(Error error) {
                Log.d("FragmentActivity", "Error" + error.toString());
                Snackbar.make(rootView, R.string.enqueue_error, Snackbar.LENGTH_INDEFINITE)
                        .show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        fetch.addListener(progressFragment1);
        fetch.addListener(progressFragment2);
        fetch.addListener(fetchListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeListener(progressFragment1);
        fetch.removeListener(progressFragment2);
        fetch.removeListener(fetchListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetch.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            enqueueDownload();

        } else {
            Snackbar.make(rootView, R.string.permission_not_enabled, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private FetchListener fetchListener = new AbstractFetchListener() {

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
            super.onProgress(download, etaInMilliseconds, downloadedBytesPerSecond);
            if (request != null && request.getId() == download.getId()) {
                Log.d("FragmentActivity", "id:" + download.getId() +
                        ",status:" + download.getStatus() + ",progress:" + download.getProgress()
                        + ",error:" + download.getError());
            }
        }

    };
}