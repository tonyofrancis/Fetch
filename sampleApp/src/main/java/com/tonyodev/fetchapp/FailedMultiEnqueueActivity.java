package com.tonyodev.fetchapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.Request;

import java.util.ArrayList;
import java.util.List;

public class FailedMultiEnqueueActivity extends AppCompatActivity {

    final static String FETCH_NAMESPACE = "FailedEnqueue";

    private Fetch fetch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_enqueue);
        final View mainView = findViewById(R.id.activity_main);
        final FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setNamespace(FETCH_NAMESPACE)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration)
                .deleteAll();

        final List<Request> requests = new ArrayList<>();
        final String url = "https://www.notdownloadable.com/test.txt";
        final int size = 15;
        for (int x = 0; x < size; x++) {
            final String file = Data.getSaveDir() + "/multiTest/file" + (x + 1) + ".txt";
            final Request request = new Request(url, file);
            requests.add(request);
        }
        fetch.enqueue(requests, null);
        Snackbar.make(mainView, "Enqueued " + size + " requests. Check Logcat for " +
                "failed status", Snackbar.LENGTH_INDEFINITE)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetch.close();
    }

}
