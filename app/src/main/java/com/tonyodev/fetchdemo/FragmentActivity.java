package com.tonyodev.fetchdemo;

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
import android.widget.Toast;

import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.listener.FetchListener;
import com.tonyodev.fetch.request.Request;
import com.tonyodev.fetch.request.RequestInfo;

import java.util.List;

/**
 * Created by tonyofrancis on 1/31/17.
 */

public class FragmentActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 150;

    private Fetch fetch;

    private View rootView;
    private ProgressFragment progressFragment1;
    private ProgressFragment progressFragment2;

    private long downloadId = Fetch.DEFAULT_EMPTY_VALUE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_progress);

        rootView = findViewById(R.id.rootView);

        fetch = Fetch.getInstance(this);

        if(savedInstanceState == null) {

            clearAllDownloads();

            progressFragment1 = new ProgressFragment();
            progressFragment2 = new ProgressFragment();

            FragmentManager fragmentManager = getSupportFragmentManager();

            fragmentManager.beginTransaction()
                    .add(R.id.fragment1,progressFragment1)
                    .add(R.id.fragment2,progressFragment2)
                    .commit();

            createRequest();
        }
    }

    /*Removes all downloads managed by Fetch*/
    private void clearAllDownloads() {

        List<RequestInfo> infos = fetch.get();

        for (RequestInfo info : infos) {
            fetch.remove(info.getId());
        }
    }

    private void createRequest() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    ,STORAGE_PERMISSION_CODE);
        }else {
            enqueueDownload();
        }
    }

    private void enqueueDownload() {

        String url = Data.sampleUrls[1];
        String filePath = Data.getSaveDir() + "/fragments/smallFile.txt";

        Request request = new Request(url,filePath);

        downloadId = fetch.enqueue(request);

        if(downloadId == -1) {
            Toast.makeText(this, R.string.enqueue_error,Toast.LENGTH_SHORT).show();
        }

        progressFragment1.setDownloadId(downloadId);
        progressFragment2.setDownloadId(downloadId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetch.addFetchListener(progressFragment1);
        fetch.addFetchListener(progressFragment2);
        fetch.addFetchListener(fetchListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeFetchListener(progressFragment1);
        fetch.removeFetchListener(progressFragment2);
        fetch.removeFetchListener(fetchListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetch.release();
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

    private FetchListener fetchListener = new FetchListener() {
        @Override
        public void onUpdate(long id, int status, int progress, long writtenBytes, long fileSize, int error) {

            if(downloadId == id) {
                Log.d("FragmentActivity","id:" + id + ",status:" + status + ",progress:" + progress
                        + ",error:" + error);
            }
        }
    };
}