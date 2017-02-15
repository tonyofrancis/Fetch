package com.tonyodev.fetchdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.listener.FetchListener;
import com.tonyodev.fetch.request.Request;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tonyofrancis on 1/29/17.
 */

public class MultiEnqueueActivity extends AppCompatActivity implements FetchListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_enqueue);

        Fetch fetch = Fetch.getInstance(this);

        List<Request> requests = new ArrayList<>();

        final String url = "https://www.notdownloadable.com/test.txt";

        final int size = 15;

        for(int x = 0; x < size; x++) {

            String filePath = Data.getSaveDir()
                    .concat("/multiTest/")
                    .concat("file")
                    .concat(""+(x+1))
                    .concat(".txt");

            Request request = new Request(url,filePath);
            requests.add(request);
        }

        fetch.enqueue(requests);

        Toast.makeText(this,"Enqueued " + size + " requests. Check Logcat for" +
                "progress status",Toast.LENGTH_LONG).show();

        fetch.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((App)getApplication()).getFetch().addFetchListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ((App)getApplication()).getFetch().removeFetchListener(this);
    }

    @Override
    public void onUpdate(long id, int status, int progress, long downloadedBytes, long fileSize, int error) {
        Log.i("MultiEnqueueActivity","Download id:" + id + " - progress:" + progress);
    }
}
