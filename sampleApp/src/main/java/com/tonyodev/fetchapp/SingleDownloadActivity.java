package com.tonyodev.fetchapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2core.DownloadBlock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import timber.log.Timber;


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
        progressTextView = findViewById(R.id.progressTextView);
        titleTextView = findViewById(R.id.titleTextView);
        etaTextView = findViewById(R.id.etaTextView);
        downloadSpeedTextView = findViewById(R.id.downloadSpeedTextView);
        fetch = Fetch.Impl.getDefaultInstance();
        checkStoragePermission();
    }


    @Override
    protected void onResume() {
        super.onResume();
        fetch.addListener(this);
        if (request != null) {
            //Refresh the screen with the downloaded data. So we perform a download query
            fetch.getDownload(request.getId(), download -> {
                if (download != null) {
                    setProgressView(download.getStatus(), download.getProgress());
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
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else {
            enqueueDownload();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enqueueDownload();
        } else {
            Snackbar.make(mainView, R.string.permission_not_enabled, Snackbar.LENGTH_LONG).show();
        }
    }

    private void enqueueDownload() {
        final String url = Data.sampleUrls[0];
        final String filePath = Data.getSaveDir() + "/movies/" + Data.getNameFromUrl(url);
        request = new Request(url, filePath);
        fetch.enqueue(request, updatedRequest -> {
            request = updatedRequest;
        }, error -> Timber.d("SingleDownloadActivity Error: %1$s", error.toString()));
    }

    private void setTitleView(@NonNull final String fileName) {
        final Uri uri = Uri.parse(fileName);
        titleTextView.setText(uri.getLastPathSegment());
    }

    private void setProgressView(@NonNull final Status status, final int progress) {
        switch (status) {
            case QUEUED: {
                progressTextView.setText(R.string.queued);
                break;
            }
            case ADDED: {
                progressTextView.setText(R.string.added);
                break;
            }
            case DOWNLOADING:
            case COMPLETED: {
                if (progress == -1) {
                    progressTextView.setText(R.string.downloading);
                } else {
                    final String progressString = getResources().getString(R.string.percent_progress, progress);
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

    private void showDownloadErrorSnackBar(@NotNull Error error) {
        final Snackbar snackbar = Snackbar.make(mainView, "Download Failed: ErrorCode: " + error, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.retry, v -> {
            fetch.retry(request.getId());
            snackbar.dismiss();
        });
        snackbar.show();
    }

    private void updateViews(@NotNull Download download, long etaInMillis, long downloadedBytesPerSecond, @Nullable Error error) {
        if (request.getId() == download.getId()) {
            setProgressView(download.getStatus(), download.getProgress());
            etaTextView.setText(Utils.getETAString(this, etaInMillis));
            downloadSpeedTextView.setText(Utils.getDownloadSpeedString(this, downloadedBytesPerSecond));
            if (error != null) {
                showDownloadErrorSnackBar(download.getError());
            }
        }
    }

    @Override
    public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
        setTitleView(download.getFile());
        setProgressView(download.getStatus(), download.getProgress());
        updateViews(download, 0, 0, null);
    }

    @Override
    public void onCompleted(@NotNull Download download) {
        updateViews(download, 0, 0, null);
    }

    @Override
    public void onError(@NotNull Download download) {
        updateViews(download, 0, 0, download.getError());
    }

    @Override
    public void onDownloadBlockUpdated(@NotNull Download download, @NotNull DownloadBlock downloadBlock, int totalBlocks) {

    }

    @Override
    public void onProgress(@NotNull Download download, long etaInMilliseconds, long downloadedBytesPerSecond) {
        updateViews(download, etaInMilliseconds, downloadedBytesPerSecond, null);
    }

    @Override
    public void onPaused(@NotNull Download download) {
        updateViews(download, 0, 0, null);
    }

    @Override
    public void onResumed(@NotNull Download download) {
        updateViews(download, 0, 0, null);
    }

    @Override
    public void onCancelled(@NotNull Download download) {
        updateViews(download, 0, 0, null);
    }

    @Override
    public void onRemoved(@NotNull Download download) {
        updateViews(download, 0, 0, null);
    }

    @Override
    public void onDeleted(@NotNull Download download) {
        updateViews(download, 0, 0, null);
    }

    @Override
    public void onAdded(@NotNull Download download) {
        setTitleView(download.getFile());
        setProgressView(download.getStatus(), download.getProgress());
        updateViews(download, 0, 0, null);
    }
}
