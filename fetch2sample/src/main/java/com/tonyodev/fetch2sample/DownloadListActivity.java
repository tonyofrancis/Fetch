package com.tonyodev.fetch2sample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.tonyodev.fetch2.callback.Callback;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.callback.Query;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.RequestData;
import com.tonyodev.fetch2.Status;
import com.tonyodev.fetch2.listener.FetchListener;

import java.io.File;
import java.util.List;

public class DownloadListActivity extends AppCompatActivity implements ActionListener,FetchListener {

    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private Fetch fetch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);
        setViews();

        fetch = Fetch.getInstance();


        fetch.queryAll(new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {

                if (result != null) {
                    for (RequestData requestData : result) {
                        File file = new File(requestData.getAbsoluteFilePath());

                        if (file.exists()) {
                            file.delete();
                        }
                    }
                }

                fetch.removeAll();
                enqueueDownloads();
            }
        });

    }

    private void setViews() {
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileAdapter = new FileAdapter(this);
        recyclerView.setAdapter(fileAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetch.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeListener(this);
    }

    private void enqueueDownloads() {

        List<Request> requests = Data.getFetchRequests();

        fetch.enqueue(requests, new Callback() {
            @Override
            public void onQueued(Request request) {

                Download download = new Download();
                download.setId(request.getId());
                download.setUrl(request.getUrl());
                download.setFilePath(request.getAbsoluteFilePath());
                download.setError(Error.NONE);
                download.setProgress(0);
                download.setStatus(Status.QUEUED);

                fileAdapter.addDownload(download);
            }

            @Override
            public void onFailure(Request request, Error reason) {

            }
        });
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

    @Override
    public void onComplete(long id, int progress, long downloadedBytes, long totalBytes) {
        fileAdapter.onUpdate(id, Status.COMPLETED,progress,downloadedBytes,totalBytes, Error.NONE);
    }

    @Override
    public void onAttach(Fetch fetch) {

        fetch.queryAll(new Query<List<RequestData>>() {
            @Override
            public void onResult(@Nullable List<RequestData> result) {
                if (result != null) {
                    for (RequestData requestData : result) {
                        fileAdapter.onUpdate(requestData.getId(),requestData.getStatus(),
                                requestData.getProgress(),requestData.getDownloadedBytes(),
                                requestData.getTotalBytes(),requestData.getError());
                    }
                }
            }
        });
    }

    @Override
    public void onDetach(Fetch fetch) {

    }

    @Override
    public void onError(long id, @NonNull Error reason, int progress, long downloadedBytes, long totalBytes) {
        fileAdapter.onUpdate(id, Status.ERROR,progress,downloadedBytes,totalBytes,reason);
    }

    @Override
    public void onProgress(long id, int progress, long downloadedBytes, long totalBytes) {
        fileAdapter.onUpdate(id, Status.DOWNLOADING,progress,downloadedBytes,totalBytes, Error.NONE);
    }

    @Override
    public void onPaused(long id, int progress, long downloadedBytes, long totalBytes) {
        fileAdapter.onUpdate(id, Status.PAUSED,progress,downloadedBytes,totalBytes, Error.NONE);
    }

    @Override
    public void onCancelled(long id, int progress, long downloadedBytes, long totalBytes) {
        fileAdapter.onUpdate(id, Status.CANCELLED,progress,downloadedBytes,totalBytes, Error.NONE);
    }

    @Override
    public void onRemoved(long id, int progress, long downloadedBytes, long totalBytes) {
        fileAdapter.onUpdate(id, Status.REMOVED,progress,downloadedBytes,totalBytes, Error.NONE);
    }
}