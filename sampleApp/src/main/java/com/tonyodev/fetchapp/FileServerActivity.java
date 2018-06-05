package com.tonyodev.fetchapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2fileserver.ContentFileRequest;
import com.tonyodev.fetch2fileserver.ContentFileServerAuthenticator;
import com.tonyodev.fetch2fileserver.FetchFileServer;

import org.jetbrains.annotations.NotNull;


public class FileServerActivity extends AppCompatActivity {

    private FetchFileServer fetchFileServer;
    private Fetch fetch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fetch = Fetch.Impl.getDefaultInstance();
        fetchFileServer = new FetchFileServer.Builder(this)
                .setClearContentFileDatabaseOnShutdown(true)
                .setAuthenticator(new ContentFileServerAuthenticator() {
            @Override
            public boolean accept(@NotNull String authorization, @NotNull ContentFileRequest contentFileRequest) {
                return authorization.equals("password");
            }
        }).build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetch.addListener(fetchListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        fetch.removeListener(fetchListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetch.close();
        fetchFileServer.shutDown(false);
    }

    private FetchListener fetchListener = new AbstractFetchListener() {
        @Override
        public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            super.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond);
        }
    };
}
