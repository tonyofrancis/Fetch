package com.tonyodev.fetchapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyodev.fetch2.AbstractFetchListener;
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2core.Downloader;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.FetchListener;
import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.Extras;
import com.tonyodev.fetch2core.FetchCoreUtils;
import com.tonyodev.fetch2core.FileResource;
import com.tonyodev.fetch2core.MutableExtras;
import com.tonyodev.fetch2fileserver.FetchFileServer;
import com.tonyodev.fetch2.FetchFileServerDownloader;
import com.tonyodev.fetch2core.FetchFileServerUriBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;


public class FileServerActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 100;
    public static final String FETCH_NAMESPACE = "fileServerActivity";
    public static final String CONTENT_PATH = "test_file.db";

    private TextView textView;
    private FetchFileServer fetchFileServer;
    private Fetch fetch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_server);
        textView = findViewById(R.id.textView);

        final FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setFileServerDownloader(new FetchFileServerDownloader(Downloader.FileDownloaderType.PARALLEL))
                .setNamespace(FETCH_NAMESPACE)
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);

        fetchFileServer = new FetchFileServer.Builder(this)
                .setAuthenticator((sessionId, authorization, fileRequest) -> authorization.equals("password"))
                .build();
        fetchFileServer.start();
        checkStoragePermission();
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else {
            copyRawFileToFilesDir();
        }
    }

    private void copyRawFileToFilesDir() {
        new Thread(() -> {
            try {
                final String testFilePath = Data.getSaveDir() + "/source/" + CONTENT_PATH;
                final File file = Utils.createFile(testFilePath);
                final InputStream inputStream = new BufferedInputStream(getResources().openRawResource(R.raw.test_file));
                final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
                final byte[] buffer = new byte[1024];
                int read;
                long readBytes = 0;
                while (((read = inputStream.read(buffer, 0, 1024)) != -1)) {
                    readBytes += read;
                    outputStream.write(buffer, 0, read);
                }
                inputStream.close();
                outputStream.flush();
                outputStream.close();
                addFileResourceToServer(file, readBytes);
            } catch (IOException exception) {
                Timber.e(exception);
            }
        }).start();
    }

    private void addFileResourceToServer(File file, long fileLength) {
        final FileResource fileResource = new FileResource();
        fileResource.setFile(file.getAbsolutePath());
        fileResource.setName(file.getName());
        fileResource.setId(file.getAbsolutePath().hashCode());
        fileResource.setLength(fileLength);
        final String fileMd5 = FetchCoreUtils.getFileMd5String(file.getAbsolutePath());
        if (fileMd5 != null) {
            fileResource.setMd5(fileMd5);
        }
        final MutableExtras extras = new MutableExtras();
        extras.putLong("contentLength", fileLength);
        extras.putString("contentName", file.getName());
        extras.putString("contentSampleKey", "contentSampleText");
        fileResource.setExtras(extras);
        fetchFileServer.addFileResource(fileResource);

        downloadFileResourceUsingFetch();
        fetch.getFetchFileServerCatalog(getCatalogRequest(),
                result -> {
                    Timber.d("Catalog:" + result.toString());
                },
                error -> Timber.d("Catalog Fetch error:" + error.toString()));
    }

    private void downloadFileResourceUsingFetch() {
        fetch.addListener(fetchListener).enqueue(getRequest(), request -> {
            Timber.d(request.toString());
        }, error -> {
            Timber.d(error.toString());
        });
    }

    private Request getRequest() {
        final String url = new FetchFileServerUriBuilder()
                .setHostInetAddress(fetchFileServer.getAddress(), fetchFileServer.getPort())
                .setFileResourceIdentifier(CONTENT_PATH)
                .toString();
        final Request request = new Request(url, getFile("(1)"));
        request.addHeader("Authorization", "password");
        request.setPriority(Priority.HIGH);
        return request;
    }

    private Request getCatalogRequest() {
        final String url = new FetchFileServerUriBuilder()
                .setHostInetAddress(fetchFileServer.getAddress(), fetchFileServer.getPort())
                .setFileResourceIdentifier("Catalog.json")
                .toString();
        final Request request = new Request(url, getFile("(1)"));
        final MutableExtras extras = new MutableExtras();
        extras.putString("sampleKey", "sampleTextDataInExtras");
        request.setExtras(extras);
        request.addHeader("Authorization", "password");
        request.setPriority(Priority.HIGH);
        return request;
    }

    private String getFile(final String incrementer) {
        return Data.getSaveDir() + "/data/ test_file" + incrementer + ".db";
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
        public void onCompleted(@NotNull Download download) {
            final String completed = "Completed Download";
            textView.setText(completed);
            Timber.d("Download From FileServer completed");
        }

        @Override
        public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            final String progress = "Progress: " + download.getProgress() + "%";
            textView.setText(progress);
            Timber.d("Download From FileServer Progress: " + download.getProgress() + "%");
        }

        @Override
        public void onError(@NotNull Download download, @NotNull Error error, @org.jetbrains.annotations.Nullable Throwable throwable) {
            super.onError(download, error, throwable);
            final String err = "Download From FileServer Error " + download.getError().toString();
            textView.setText(err);
            Timber.d(err);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            copyRawFileToFilesDir();
        } else {
            Toast.makeText(this, R.string.permission_not_enabled, Toast.LENGTH_LONG).show();
        }
    }

}
