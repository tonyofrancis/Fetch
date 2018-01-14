package com.tonyodev.fetchapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Func;
import com.tonyodev.fetch2.Func2;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.Status;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class SingleDownloadActivity extends AppCompatActivity implements FetchListener {

    private static final int STORAGE_PERMISSION_CODE = 100;

    private View mainView;
    private TextView progressTextView;
    private TextView titleTextView;
    private TextView etaTextView;
    private TextView downloadSpeedTextView;

    private Request request;
    private Fetch fetch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_download);
        mainView = findViewById(R.id.activity_single_download);
        progressTextView = (TextView) findViewById(R.id.progressTextView);
        titleTextView = (TextView) findViewById(R.id.titleTextView);
        etaTextView = (TextView) findViewById(R.id.etaTextView);
        downloadSpeedTextView = (TextView) findViewById(R.id.downloadSpeedTextView);
        fetch = ((App) getApplication()).getFetch();
        fetch.deleteAll();
        checkStoragePermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetch.addListener(this);
        if (request != null) {
            fetch.getDownload(request.getId(), new Func2<Download>() {
                @Override
                public void call(@Nullable Download download) {
                    if (download != null) {
                        setProgressView(download.getStatus(), download.getProgress());
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetch.close();
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    , STORAGE_PERMISSION_CODE);
        } else {
            enqueueDownload();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            enqueueDownload();

        } else {
            Snackbar.make(mainView, R.string.permission_not_enabled, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    private void enqueueDownload() {
        final String url = Data.sampleUrls[0];
        final String filePath = Data.getSaveDir() + "/movies/buckbunny" + System.nanoTime() + ".m4v";

        request = new Request(url, filePath);
        fetch.enqueue(request, new Func<Download>() {
            @Override
            public void call(Download download) {
                setTitleView(download.getFile());
                setProgressView(download.getStatus(), download.getProgress());
            }
        }, new Func<Error>() {
            @Override
            public void call(Error error) {
                Log.d("SingleDownloadActivity", "Error:" + error.toString());
            }
        });
    }

    private void setTitleView(String fileName) {
        final Uri uri = Uri.parse(fileName);
        titleTextView.setText(uri.getLastPathSegment());
    }

    private void setProgressView(Status status, int progress) {
        switch (status) {
            case QUEUED: {
                progressTextView.setText(R.string.queued);
                break;
            }
            case DOWNLOADING:
            case COMPLETED: {
                if (progress == -1) {
                    progressTextView.setText(R.string.downloading);
                } else {
                    final String progressString = getResources()
                            .getString(R.string.percent_progress, progress);
                    progressTextView.setText(progressString);
                }
                break;
            }
            default: {
                progressTextView.setText(R.string.status_unknown);
                break;
            }
        }
    }

    private void showDownloadErrorSnackBar(Error error) {
        final Snackbar snackbar = Snackbar.make(mainView, "Download Failed: ErrorCode: "
                + error, Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (request != null) {
                    fetch.retry(request.getId());
                    snackbar.dismiss();
                }
            }
        });

        snackbar.show();
    }

    @Override
    public void onQueued(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(Utils
                    .getETAString(SingleDownloadActivity.this, 0));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(SingleDownloadActivity.this, 0));
        }
    }

    @Override
    public void onCompleted(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(Utils
                    .getETAString(SingleDownloadActivity.this, 0));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(SingleDownloadActivity.this, 0));
        }
    }

    @Override
    public void onError(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            showDownloadErrorSnackBar(download.getError());
            etaTextView.setText(Utils
                    .getETAString(SingleDownloadActivity.this, 0));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(SingleDownloadActivity.this, 0));
        }
    }

    @Override
    public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
        if (request != null && request.getId() == download.getId()) {
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(Utils
                    .getETAString(SingleDownloadActivity.this, etaInMilliseconds));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(SingleDownloadActivity.this, downloadedBytesPerSecond));
        }
    }

    @Override
    public void onPaused(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(Utils
                    .getETAString(SingleDownloadActivity.this, 0));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(SingleDownloadActivity.this, 0));
        }
    }

    @Override
    public void onResumed(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(Utils
                    .getETAString(SingleDownloadActivity.this, 0));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(SingleDownloadActivity.this, 0));
        }
    }

    @Override
    public void onCancelled(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(Utils
                    .getETAString(SingleDownloadActivity.this, 0));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(SingleDownloadActivity.this, 0));
        }
    }

    @Override
    public void onRemoved(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(Utils
                    .getETAString(SingleDownloadActivity.this, 0));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(SingleDownloadActivity.this, 0));
        }
    }

    @Override
    public void onDeleted(@NotNull Download download) {
        if (request != null && request.getId() == download.getId()) {
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(Utils
                    .getETAString(SingleDownloadActivity.this, 0));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(SingleDownloadActivity.this, 0));
        }
    }

}
