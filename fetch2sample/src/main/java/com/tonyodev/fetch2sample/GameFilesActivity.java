package com.tonyodev.fetch2sample;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyodev.fetch2.Callback;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.listener.FetchListener;

import java.util.List;


public class GameFilesActivity extends AppCompatActivity {

    private TextView progressTextView;
    private ProgressBar progressBar;
    private Button startButton;
    private TextView labelTextView;

    private List<Request> requestList;
    private int completed = 0;
    private int removeCount = 0;
    private Fetch fetch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_files);
        setViews();

        requestList = Data.getGameUpdates();
        fetch = Fetch.getInstance();

        fetch.addListener(fetchListener);

        deleteFiles();
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
                    startButton.setEnabled(false);
                    startButton.setText("downloading...");
                    labelTextView.setText(R.string.fetch_started);
                    enqueueFiles();
                }
            }
        });
    }

    private void deleteFiles() {
       fetch.deleteGroup("gameFiles");
    }

    private void updateUI() {

        int totalFiles = requestList.size();
        int completedFiles = completed;

        progressTextView.setText(getResources()
                .getString(R.string.complete_over,completedFiles,totalFiles));

        int progress = getDownloadProgress();

        progressBar.setProgress(progress);

        if(completedFiles == totalFiles) {
            labelTextView.setText(getString(R.string.fetch_done));
            startButton.setText(R.string.reset);
            startButton.setEnabled(true);
            startButton.setVisibility(View.VISIBLE);
        }
    }

    private int getDownloadProgress() {
        return (int) (((double) completed / (double)requestList.size()) * 100);
    }
    private void reset() {
        deleteFiles();
        startButton.setEnabled(true);
        completed = 0;
        removeCount = 0;
        progressBar.setProgress(0);
        progressTextView.setText("");
        labelTextView.setText(R.string.start_fetching);
        startButton.setText(R.string.start);
        startButton.setVisibility(View.VISIBLE);
    }

    private void enqueueFiles() {

        fetch.enqueue(requestList, new Callback() {
            @Override
            public void onQueued(Request request) {
                Log.d("onQueued",request.toString());
            }

            @Override
            public void onFailure(Request request, Error reason) {
                Log.d("onFailure",request.toString());
            }
        });
    }

    private final FetchListener fetchListener = new FetchListener() {
        @Override
        public void onAttach(Fetch fetch) {
        }

        @Override
        public void onDetach(Fetch fetch) {
        }

        @Override
        public void onComplete(long id, int progress, long downloadedBytes, long totalBytes) {
            completed++;
            updateUI();
        }

        @Override
        public void onError(long id, Error reason, int progress, long downloadedBytes, long totalBytes) {
            reset();
            Toast.makeText(GameFilesActivity.this, R.string.game_download_error,
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProgress(long id, int progress, long downloadedBytes, long totalBytes) {

        }

        @Override
        public void onPaused(long id, int progress, long downloadedBytes, long totalBytes) {

        }

        @Override
        public void onCancelled(long id, int progress, long downloadedBytes, long totalBytes) {

        }

        @Override
        public void onRemoved(long id, int progress, long downloadedBytes, long totalBytes) {

            if (++removeCount == requestList.size()) {
                updateUI();
                removeCount = 0;
                completed = 0;
            }
        }
    };
}
