package com.tonyodev.fetchapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.NetworkType;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;

public class TestActivity extends AppCompatActivity {

    private Fetch fetch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FetchConfiguration fetchConfiguration = new FetchConfiguration
                .Builder(this)
                .build();

        fetch = Fetch.Impl.getInstance(fetchConfiguration);

        String url = "http:www.example.com/test.txt";
        String file = "/downloads/test.txt";
        final Request request = new Request(url, file);
        request.setPriority(Priority.HIGH);
        request.setNetworkType(NetworkType.ALL);
        request.addHeader("clientKey", "SD78DF93_3947&MVNGHE1WONG");

        fetch.enqueue(request, updatedRequest -> {
            //Request was successfully enqueued for download
        }, error -> {
            //An error occurred enqueuing the request.
        });

    }
}
