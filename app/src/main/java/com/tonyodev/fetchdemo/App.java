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
        fetch = Fetch.getInstance(this);

        fetch.addFetchListener(new FetchListener() {
            @Override
            public void onUpdate(long id, int status, int progress, int error) {

                Log.d("fetchDebug","id:" + id + ",status:" + status + ",progress:" + progress
                + ",error:" + error);

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
