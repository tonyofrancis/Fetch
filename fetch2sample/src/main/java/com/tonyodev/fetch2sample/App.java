package com.tonyodev.fetch2sample;

import android.app.Application;

import com.tonyodev.fetch2.Fetch;

import okhttp3.OkHttpClient;


/**
 * Created by tonyofrancis onQueued 1/30/17.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fetch.init(this,new OkHttpClient());
    }
}
