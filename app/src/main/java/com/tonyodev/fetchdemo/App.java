package com.tonyodev.fetchdemo;

import android.app.Application;
import android.util.Log;

import com.tonyodev.fetch.Fetch;
import com.tonyodev.fetch.listener.FetchListener;

/**
 * Created by tonyofrancis on 1/30/17.
 */

public class App extends Application {

    private Fetch fetch;

    @Override
    public void onCreate() {
        super.onCreate();

        //Set settings for Fetch
        new Fetch.Settings(this)
                .setAllowedNetwork(Fetch.NETWORK_ALL)
                .enableLogging(true)
                .setConcurrentDownloadsLimit(1)
                .setFollowSslRedirects(true)
                .apply();

        fetch = Fetch.getInstance(this);

        fetch.addFetchListener(new FetchListener() {
            @Override
            public void onUpdate(long id, int status, int progress, long downloadedBytes, long fileSize, int error) {

                Log.d("fetchDebug","id:" + id + ",status:" + status + ",progress:" + progress
                + ",error:" + error);

                Log.i("fetchDebug","id: " + id + " downloadedBytes: " + downloadedBytes + " / fileSize: " + fileSize);
            }
        });

        /*
        * New Feature - FetchCall
        * FetchCall Example
        * */
        /*
        String url = Data.sampleUrls[1];
        Request request = new Request(url);

        Fetch.call(request, new FetchCall<String>() {

            @Override
            public void onSuccess(@Nullable String response, @NonNull Request request) {

                Log.d("fetchCall",response);
            }

            @Override
            public void onError(int error,@NonNull Request request) {
                Log.d("fetchCall","error: " + error + "for request:" + request.toString());
            }
        });

       // Fetch.cancelCall(request);

       */
    }

    public Fetch getFetch() {
        return fetch;
    }
}
