package com.tonyodev.fetch2sample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.tonyodev.fetch2.callback.Callback;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2.listener.AbstractFetchListener;
import com.tonyodev.fetch2.listener.FetchListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tonyofrancis on 1/29/17.
 */

public class MultiEnqueueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_enqueue);

        final Fetch fetch = Fetch.getInstance();

        List<Request> requests = new ArrayList<>();

        final String url = "https://www.notdownloadable.com/test.txt";
        final int size = 50;

        for(int x = 0; x < size; x++) {

            String filePath = Data.getSaveDir()
                    .concat("/multiTest/")
                    .concat("file")
                    .concat(""+(x+1))
                    .concat(".txt");

            Request request = new Request(url,filePath);
            request.setGroupId("multiEnqueue");
            requests.add(request);
        }

        fetch.deleteGroup("multiEnqueue");
        fetch.enqueue(requests, new Callback() {
            @Override
            public void onQueued(@NonNull Request request) {
                Log.d("onQueued",request.toString());
            }

            @Override
            public void onFailure(@NonNull Request request, @NonNull Error reason) {
                Log.d("onFailure",reason.toString());
            }
        });

        Toast.makeText(MultiEnqueueActivity.this,"Enqueued " + size + " requests. Check Logcat for" +
                "progress status",Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Fetch.getInstance().addListener(listener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Fetch.getInstance().removeListener(listener);
    }

    private final FetchListener listener = new AbstractFetchListener() {
        @Override
        public void onProgress(long id, int progress, long downloadedBytes, long totalBytes) {
            Log.i("MultiEnqueueActivity","Download id:" + id + " - progress:" + progress);
        }

        @Override
        public void onError(long id, @NonNull Error reason, int progress, long downloadedBytes, long totalBytes) {
            Log.d("MultiEnqueueActivity","Download id:" + id + " - error:" + reason.toString());
        }
    };
}
