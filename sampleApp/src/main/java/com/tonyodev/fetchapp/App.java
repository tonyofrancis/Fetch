package com.tonyodev.fetchapp;

import android.app.Application;
import android.support.annotation.NonNull;

import com.tonyodev.fetch2.Downloader;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.Logger;
import com.tonyodev.fetch2.RequestOptions;
import com.tonyodev.fetch2downloaders.OkHttpDownloader;
import com.tonyodev.fetch2rx.RxFetch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import timber.log.Timber;

public class App extends Application {

    public static final String APP_FETCH_NAMESPACE = "DefaultFetch";
    public static final String GAMES_FETCH_NAMESPACE = "GameFilesFetch";

    @Nullable
    private Fetch fetch;
    @Nullable
    private RxFetch rxFetch;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    @NonNull
    public Fetch getAppFetchInstance() {
        if (fetch == null || fetch.isClosed()) {
            fetch = getNewFetchInstance(APP_FETCH_NAMESPACE);
        }
        return fetch;
    }

    @NonNull
    public Fetch getNewFetchInstance(@NonNull final String namespace) {
        final OkHttpClient client = new OkHttpClient.Builder().build();
        final Downloader okHttpDownloader = new OkHttpDownloader(client);
        return new Fetch.Builder(this, namespace)
                .setLogger(new FetchTimberLogger())
                .setDownloader(okHttpDownloader)
                .setDownloadConcurrentLimit(1)
                .enableLogging(true)
                .enableRetryOnNetworkGain(true)
                .addRequestOptions(RequestOptions.REPLACE_ON_ENQUEUE)
                .build();
    }

    @NonNull
    public RxFetch getRxFetch() {
        if (rxFetch == null || rxFetch.isClosed()) {
            final OkHttpClient client = new OkHttpClient.Builder().build();
            rxFetch = new RxFetch.Builder(this, GAMES_FETCH_NAMESPACE)
                    .setDownloader(new OkHttpOutputStreamDownloader(client))
                    .setDownloadConcurrentLimit(1)
                    .enableLogging(true)
                    .addRequestOptions(RequestOptions.REPLACE_ON_ENQUEUE_FRESH)
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

    /**
     * Customer downloader that lets you provide your own output streams for downloads.
     * See Downloader.kt documentation for more information on providing your own downloader.
     */
    private static class OkHttpOutputStreamDownloader extends OkHttpDownloader {

        public OkHttpOutputStreamDownloader() {
            super(null);
        }

        public OkHttpOutputStreamDownloader(@Nullable OkHttpClient okHttpClient) {
            super(okHttpClient);
        }

        @Nullable
        @Override
        public OutputStream getRequestOutputStream(@NotNull Request request, long filePointerOffset) {
            //If overriding this method, see the Downloader.kt documentation on how to properly use this method.
            // If done incorrectly you may override data in files.
            try {
                final FileOutputStream fileOutputStream = new FileOutputStream(request.getFile(), true);
                return new BufferedOutputStream(fileOutputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                //Cannot find file. Provide fallback.
            }
            return null;
        }

    }

}
