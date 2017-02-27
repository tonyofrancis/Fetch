package com.tonyodev.fetchdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.request.Request;
import com.tonyodev.fetch.request.RequestInfo;

import java.util.List;

public class DownloadListActivity extends AppCompatActivity implements ActionListener {

    private static final int STORAGE_PERMISSION_CODE = 200;

    private RecyclerView recyclerView;
    private SwitchCompat networkSwitch;

    private FileAdapter fileAdapter;
    private Fetch fetch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);
        setViews();

        fetch = Fetch.getInstance(this);
        clearAllDownloads();
    }

    /*Removes all downloads managed by Fetch*/
    private void clearAllDownloads() {

        Fetch fetch = Fetch.getInstance(this);
        fetch.removeAll();

        createNewRequests();
        fetch.release();
    }

    private void setViews() {

        networkSwitch = (SwitchCompat) findViewById(R.id.networkSwitch);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        networkSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked) {
                    fetch.setAllowedNetwork(Fetch.NETWORK_WIFI);
                }else {
                    fetch.setAllowedNetwork(Fetch.NETWORK_ALL);
                }
            }
        });

        fileAdapter = new FileAdapter(this);
        recyclerView.setAdapter(fileAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        List<RequestInfo> infos = fetch.get();

        for (RequestInfo info : infos) {

            fileAdapter.onUpdate(info.getId(), info.getStatus()
                    , info.getProgress(),info.getDownloadedBytes(),info.getFileSize(),info.getError());
        }

        fetch.addFetchListener(fileAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeFetchListener(fileAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetch.release();
    }

    private void createNewRequests() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }else {
            enqueueDownloads();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == STORAGE_PERMISSION_CODE || grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            enqueueDownloads();

        }else {
            Toast.makeText(this,R.string.permission_not_enabled,Toast.LENGTH_SHORT).show();
        }
    }

    private void enqueueDownloads() {

        List<Request> requests = Data.getFetchRequests();
        List<Long> ids = fetch.enqueue(requests);

        for (int i = 0; i < requests.size(); i++) {

            Request request = requests.get(i);
            long id = ids.get(i);

            Download download = new Download();
            download.setId(id);
            download.setUrl(request.getUrl());
            download.setFilePath(request.getFilePath());
            download.setError(Fetch.DEFAULT_EMPTY_VALUE);
            download.setProgress(0);
            download.setStatus(Fetch.STATUS_QUEUED);

            fileAdapter.addDownload(download);
        }
    }

    @Override
    public void onPauseDownload(long id) {
        fetch.pause(id);
    }

    @Override
    public void onResumeDownload(long id) {
        fetch.resume(id);
    }

    @Override
    public void onRemoveDownload(long id) {
        fetch.remove(id);
    }

    @Override
    public void onRetryDownload(long id) {
        fetch.retry(id);
    }
}