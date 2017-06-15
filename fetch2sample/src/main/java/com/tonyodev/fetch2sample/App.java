package com.tonyodev.fetch2sample;

import android.app.Application;

import com.tonyodev.fetch2.Fetch;


/**
 * Created by tonyofrancis onQueued 1/30/17.
 */

public class App extends Application {

    private Fetch fetch;

    @Override
    public void onCreate() {
        super.onCreate();
        fetch = Fetch.getDefaultInstance(this);
    }

    public Fetch getFetch() {
        return fetch;
    }
}
