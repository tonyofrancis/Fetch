package com.tonyodev.fetchapp;

import android.app.Application;

import com.tonyodev.fetch2.Downloader;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Logger;
import com.tonyodev.fetch2downloaders.OkHttpDownloader;
import com.tonyodev.fetch2rx.RxFetch;

import org.jetbrains.annotations.NotNull;

import okhttp3.OkHttpClient;
import timber.log.Timber;

public class App extends Application {

    private Fetch fetch;
    private RxFetch rxFetch;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    public Fetch getFetch() {
        if (fetch == null || fetch.isClosed()) {
            final OkHttpClient client = new OkHttpClient.Builder().build();
            final Downloader okHttpDownloader = new OkHttpDownloader(client);
            final Logger logger = new FetchTimberLogger();
            final int concurrentLimit = 2;
            final boolean enableLogging = true;
            fetch = new Fetch.Builder(this, "DefaultFetch")
                    .setLogger(logger)
                    .setDownloader(okHttpDownloader)
                    .setDownloadConcurrentLimit(concurrentLimit)
                    .enableLogging(enableLogging)
                    .enableRetryOnNetworkGain(true)
                    .build();
        }
        return fetch;
    }

    public RxFetch getRxFetch() {
        if (rxFetch == null || rxFetch.isClosed()) {
            final Logger logger = new FetchTimberLogger();
            final int concurrentLimit = 2;
            final boolean enableLogging = true;
            rxFetch = new RxFetch.Builder(this, "GameFilesFetch")
                    .setLogger(logger)
                    .setDownloadConcurrentLimit(concurrentLimit)
                    .enableLogging(enableLogging)
                    .build();
        }
        return rxFetch;
    }

    /* Example for custom Fetch logger using Timber.*/
    private static class FetchTimberLogger implements Logger {

        private boolean enabled;

        @Override
        public boolean getEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void d(@NotNull String message) {
            if (enabled) {
                Timber.d(message);
            }
        }

        @Override
        public void d(@NotNull String message, @NotNull Throwable throwable) {
            if (enabled) {
                Timber.d(throwable);
            }
        }

        @Override
        public void e(@NotNull String message) {
            if (enabled) {
                Timber.e(message);
            }
        }

        @Override
        public void e(@NotNull String message, @NotNull Throwable throwable) {
            if (enabled) {
                Timber.e(throwable);
            }
        }
    }

}
