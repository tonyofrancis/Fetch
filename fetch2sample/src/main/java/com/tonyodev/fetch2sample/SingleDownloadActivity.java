package com.tonyodev.fetch2sample;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyodev.fetch2.Callback;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Query;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.RequestData;
import com.tonyodev.fetch2.listener.AbstractFetchListener;
import com.tonyodev.fetch2.listener.FetchListener;

import java.io.File;

public class SingleDownloadActivity extends AppCompatActivity {
    private TextView progressTextView;
    private TextView titleTextView;

    private Fetch fetch;
    private Request request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpViews();

        fetch = Fetch.getInstance();
        request = createRequest();
        deleteFileIfExist();

        fetch.addListener(listener);
        fetch.remove(request.getId());
        fetch.enqueue(request,new Callback() {
            @Override
            public void onQueued(Request request) {
                Log.d("onQueued",request.toString());
                setTitleView(request.getAbsoluteFilePath());
                setDownloadProgressView(0);
            }
            @Override
            public void onFailure(Request request, Error reason) {
                progressTextView.setText("Enqueue Request: " + request.toString() + "\nFailed: Error:" + reason);
            }
        });
    }

    private void setUpViews() {
        setContentView(R.layout.activity_single_download);
        progressTextView = (TextView) findViewById(R.id.progressTextView);
        titleTextView = (TextView) findViewById(R.id.titleTextView);
    }

    private void deleteFileIfExist() {
        File file = new File(request.getAbsoluteFilePath());

        if(file.exists()) {
            file.delete();
        }
    }

    private void setTitleView(String fileName) {
        Uri uri = Uri.parse(fileName);
        titleTextView.setText(uri.getLastPathSegment());
    }

    private void setDownloadProgressView(int progress) {
        String progressString = getResources()
                .getString(R.string.percent_progress,progress);

        progressTextView.setText(progressString);
    }

    private Request createRequest() {
        return new Request(Data.sampleUrls[1], Data.getSaveDir() + "/files/zips.json");
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetch.addListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeListener(listener);
    }

    private final FetchListener listener = new AbstractFetchListener() {
        @Override
        public void onComplete(long id, int progress, long downloadedBytes, long totalBytes) {
            if(request.getId() == id) {
                setDownloadProgressView(progress);
                Toast.makeText(SingleDownloadActivity.this,"Download Completed",Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onError(long id, Error reason, int progress, long downloadedBytes, long totalBytes) {
            if(request.getId() == id) {
                progressTextView.setText("Enqueue Request: " + request.toString() + "\nFailed: Error:" + reason);
            }
        }

        @Override
        public void onProgress(long id, int progress, long downloadedBytes, long totalBytes) {
            if(request.getId() == id) {
                setDownloadProgressView(progress);
                Log.d("onProgress",progress+"%");
            }
        }

        @Override
        public void onAttach(Fetch fetch) {
            fetch.query(request.getId(), new Query<RequestData>() {
                @Override
                public void onResult(@Nullable RequestData result) {
                    if(result != null) {
                        setDownloadProgressView(result.getProgress());
                    }
                }
            });
        }
    };
}