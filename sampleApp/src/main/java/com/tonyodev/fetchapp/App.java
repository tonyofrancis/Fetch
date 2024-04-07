package com.tonyodev.fetchapp;

import androidx.multidex.MultiDexApplication;

import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.HttpUrlConnectionDownloader;
import com.tonyodev.fetch2core.Downloader;
import com.tonyodev.fetch2rx.RxFetch;

public class App extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        TimberUtils.configTimber();
        final FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .enableRetryOnNetworkGain(true)
                .setDownloadConcurrentLimit(3)
                .setHttpDownloader(new HttpUrlConnectionDownloader(Downloader.FileDownloaderType.PARALLEL))
                .build();
        Fetch.Impl.setDefaultInstanceConfiguration(fetchConfiguration);
        RxFetch.Impl.setDefaultRxInstanceConfiguration(fetchConfiguration);
    }
}
