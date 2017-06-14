package com.tonyodev.fetch2sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Request;

/**
 * Created by tonyofrancis on 1/31/17.
 */

public class FragmentActivity extends AppCompatActivity {

    private Fetch fetch;
    private Request request;

    private View rootView;
    private ProgressFragment progressFragment1;
    private ProgressFragment progressFragment2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_progress);
        rootView = findViewById(R.id.rootView);

        fetch = Fetch.getDefaultInstance(this);

        FragmentManager fragmentManager = getSupportFragmentManager();

        if(savedInstanceState == null) {

            progressFragment1 = new ProgressFragment();
            progressFragment2 = new ProgressFragment();


            fragmentManager.beginTransaction()
                    .add(R.id.fragment1,progressFragment1)
                    .add(R.id.fragment2,progressFragment2)
                    .commit();

        }else {
            progressFragment1 = (ProgressFragment) fragmentManager.findFragmentById(R.id.fragment1);
            progressFragment2 = (ProgressFragment) fragmentManager.findFragmentById(R.id.fragment2);
        }


        fetch.removeAll();
        enqueueDownload();
    }

    private void enqueueDownload() {

        String url = Data.sampleUrls[0];
        String filePath = Data.getSaveDir() + "/fragments/smallFile.txt";

        request = new Request(url,filePath);

        progressFragment1.setRequest(request);
        progressFragment2.setRequest(request);

        fetch.download(request);
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
        fetch.dispose();
    }

    private FetchListener fetchListener = new FetchListener() {

        @Override
        public void onComplete(long id, int progress, long downloadedBytes, long totalBytes) {

        }

        @Override
        public void onAttach(Fetch fetch) {

        }

        @Override
        public void onDetach(Fetch fetch) {

        }

        @Override
        public void onError(long id, Error reason, int progress, long downloadedBytes, long totalBytes) {
            if (id == request.getId()) {
                Log.d("FragmentActivity",",error:" + reason.toString());
            }
        }

        @Override
        public void onProgress(long id, int progress, long downloadedBytes, long totalBytes) {
            if (id == request.getId()) {
                Log.d("FragmentActivity",",progress:" + progress);
            }
        }

        @Override
        public void onPause(long id, int progress, long downloadedBytes, long totalBytes) {

        }

        @Override
        public void onCancelled(long id, int progress, long downloadedBytes, long totalBytes) {

        }

        @Override
        public void onRemoved(long id, int progress, long downloadedBytes, long totalBytes) {

        }
    };
}