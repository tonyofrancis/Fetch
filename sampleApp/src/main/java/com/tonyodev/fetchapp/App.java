package com.tonyodev.fetchapp;

import android.app.Application;

import com.tonyodev.fetch2core.Downloader;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2downloaders.OkHttpDownloader;
import com.tonyodev.fetch2fileserver.FetchFileServerDownloader;

import timber.log.Timber;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        final FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .enableRetryOnNetworkGain(true)
                .setDownloadConcurrentLimit(3)
                .setHttpDownloader(new OkHttpDownloader(Downloader.FileDownloaderType.PARALLEL))
                .setFileServerDownloader(new FetchFileServerDownloader(Downloader.FileDownloaderType.PARALLEL))
                .build();
        Fetch.Impl.setDefaultInstanceConfiguration(fetchConfiguration);
    }

}
