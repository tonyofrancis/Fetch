package com.tonyodev.fetchdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.listener.FetchListener;
import com.tonyodev.fetch.request.Request;
import com.tonyodev.fetch.request.RequestInfo;

import java.util.List;
import java.util.Set;

public class GameFilesActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 400;

    private TextView progressTextView;
    private ProgressBar progressBar;
    private Button startButton;
    private TextView labelTextView;

    private final ArrayMap<Long,Integer> fileProgress = new ArrayMap<>();

    private Fetch fetch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_files);
        setViews();

        fetch = Fetch.getInstance(this);
    }

    private void setViews() {

        progressTextView = (TextView) findViewById(R.id.progressTextView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        startButton = (Button) findViewById(R.id.startButton);
        labelTextView = (TextView) findViewById(R.id.labelTextView);

        //Start downloads
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String label = (String) startButton.getText();
                Context context = GameFilesActivity.this;

                if(label.equals(context.getString(R.string.reset))) {
                    reset();
                }else {

                    startButton.setVisibility(View.GONE);
                    labelTextView.setText(R.string.fetch_started);
                    createRequests();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(fileProgress.size() == 0) {
            fetch.addFetchListener(fetchListener);
        } else {

            List<RequestInfo> infos = fetch.get();

            for (RequestInfo info : infos) {

                if(fileProgress.containsKey(info.getId())) {
                    fileProgress.put(info.getId(),info.getProgress());
                }
            }

            updateUI();
            fetch.addFetchListener(fetchListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeFetchListener(fetchListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeDownloads();
        fetch.release();
    }

    private void createRequests() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }else {
            enqueueFiles();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == STORAGE_PERMISSION_CODE || grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            enqueueFiles();

        }else {
            Toast.makeText(this,R.string.permission_not_enabled,Toast.LENGTH_SHORT).show();
            reset();
        }
    }

    private void removeDownloads() {

        Set<Long> ids = fileProgress.keySet();

        for (Long id : ids) {
            fetch.remove(id);
        }
    }

    private void updateUI() {

        int totalFiles = fileProgress.size();
        int completedFiles = getCompletedFileCount();

        progressTextView.setText(getResources()
                .getString(R.string.complete_over,completedFiles,totalFiles));

        int progress = getDownloadProgress();

        progressBar.setProgress(progress);

        if(completedFiles == totalFiles) {
            labelTextView.setText(getString(R.string.fetch_done));
            startButton.setText(R.string.reset);
            startButton.setVisibility(View.VISIBLE);
        }
    }

    private int getDownloadProgress() {

        int currentProgress = 0;
        int totalProgress = fileProgress.size() * 100;

        Set<Long> ids = fileProgress.keySet();

        for (Long id : ids) {

            currentProgress += fileProgress.get(id);
        }

        currentProgress = (int) (((double) currentProgress / (double)totalProgress) * 100);

        return currentProgress;
    }

    private int getCompletedFileCount() {

        int count = 0;

        Set<Long> ids = fileProgress.keySet();

        for (Long id : ids) {

            int progress = fileProgress.get(id);

            if(progress == 100) {
                count++;
            }
        }

        return count;
    }

    private void reset() {

        removeDownloads();
        fileProgress.clear();
        progressBar.setProgress(0);
        progressTextView.setText("");
        labelTextView.setText(R.string.start_fetching);
        startButton.setText(R.string.start);
        startButton.setVisibility(View.VISIBLE);
    }

    private void enqueueFiles() {

        List<Request> requestList = Data.getGameUpdates();

        for (Request request : requestList) {
            long id = fetch.enqueue(request);
            fileProgress.put(id,0);
            updateUI();
        }
    }

    private final FetchListener fetchListener = new FetchListener() {
        @Override
        public void onUpdate(long id, int status, int progress, long downloadedBytes, long fileSize, int error) {

            if(fileProgress.containsKey(id)) {

                switch (status) {
                    case Fetch.STATUS_DOWNLOADING:
                    case Fetch.STATUS_DONE: {
                        fileProgress.put(id,progress);
                        updateUI();
                        break;
                    }
                    case Fetch.STATUS_ERROR: {
                        reset();
                        Toast.makeText(GameFilesActivity.this,R.string.game_download_error,
                                Toast.LENGTH_SHORT).show();
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    };
}
