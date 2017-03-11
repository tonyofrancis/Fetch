package com.tonyodev.fetchdemo;

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

import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.listener.FetchListener;
import com.tonyodev.fetch.request.Request;
import com.tonyodev.fetch.request.RequestInfo;

public class SingleDownloadActivity extends AppCompatActivity implements FetchListener {

    private static final int STORAGE_PERMISSION_CODE = 100;

    private View rootView;
    private TextView progressTextView;
    private TextView titleTextView;

    private long downloadId = -1;
    private Fetch fetch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_download);

        rootView = findViewById(R.id.activity_single_download);
        progressTextView = (TextView) findViewById(R.id.progressTextView);
        titleTextView = (TextView) findViewById(R.id.titleTextView);

        fetch = Fetch.getInstance(this);
        clearAllDownloads();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(downloadId != -1) {

            RequestInfo info = fetch.get(downloadId);

            if (info != null) {
                setProgressView(info.getStatus(),info.getProgress());
            }

            fetch.addFetchListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeFetchListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetch.release();
    }

    /*Removes all downloads managed by Fetch*/
    private void clearAllDownloads() {

        fetch.removeAll();

        createRequest();
    }

    private void createRequest() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    ,STORAGE_PERMISSION_CODE);
        }else {
            enqueueDownload();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == STORAGE_PERMISSION_CODE || grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            enqueueDownload();

        }else {
            Snackbar.make(rootView,R.string.permission_not_enabled,Snackbar.LENGTH_LONG).show();
        }
    }

    private void enqueueDownload() {

        String url = Data.sampleUrls[0];
        String filePath = Data.getSaveDir() + "/movies/buckbunny" + System.nanoTime() + ".m4v";

        Request request = new Request(url,filePath);

        downloadId = fetch.enqueue(request);

        setTitleView(request.getFilePath());
        setProgressView(Fetch.STATUS_QUEUED,0);
    }

    @Override
    public void onUpdate(long id, int status, int progress, long downloadedBytes, long fileSize, int error) {

        if(id == downloadId) {

            if(status == Fetch.STATUS_ERROR) {

                showDownloadErrorSnackBar(error);

            }else {

                setProgressView(status,progress);
            }
        }
    }

    private void setTitleView(String fileName) {

        Uri uri = Uri.parse(fileName);
        titleTextView.setText(uri.getLastPathSegment());
    }

    private void setProgressView(int status,int progress) {


        switch (status) {

            case Fetch.STATUS_QUEUED : {
                progressTextView.setText(R.string.queued);
                break;
            }
            case Fetch.STATUS_DOWNLOADING :
            case Fetch.STATUS_DONE : {

                if(progress == -1) {

                    progressTextView.setText(R.string.downloading);
                }else {

                    String progressString = getResources()
                            .getString(R.string.percent_progress,progress);

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

    private void showDownloadErrorSnackBar(int error) {

        final Snackbar snackbar = Snackbar.make(rootView,"Download Failed: ErrorCode: "
                + error,Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fetch.retry(downloadId);
                snackbar.dismiss();
            }
        });

        snackbar.show();
    }
}
