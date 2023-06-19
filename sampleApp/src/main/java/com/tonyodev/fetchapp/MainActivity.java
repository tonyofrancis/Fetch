package com.tonyodev.fetchapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2fileserver.FetchFileServer;
import com.tonyodev.fetch2rx.RxFetch;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 50;

    private View mainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainView = findViewById(R.id.activity_main);

        findViewById(R.id.singleDemoButton).setOnClickListener(v -> {
            final Intent intent = new Intent(MainActivity.this, SingleDownloadActivity.class);
            MainActivity.this.startActivity(intent);
        });

        findViewById(R.id.downloadListButton).setOnClickListener(v -> {
            final Intent intent = new Intent(MainActivity.this, DownloadListActivity.class);
            MainActivity.this.startActivity(intent);
        });

        findViewById(R.id.gameFilesButton).setOnClickListener(v -> {
            final Intent intent = new Intent(MainActivity.this, GameFilesActivity.class);
            MainActivity.this.startActivity(intent);
        });

        findViewById(R.id.multiEnqueueButton).setOnClickListener(v -> {
            final Intent intent = new Intent(MainActivity.this, FailedMultiEnqueueActivity.class);
            MainActivity.this.startActivity(intent);
        });

        findViewById(R.id.multiFragmentButton).setOnClickListener(v -> {
            final Intent intent = new Intent(MainActivity.this, FragmentActivity.class);
            MainActivity.this.startActivity(intent);
        });

        findViewById(R.id.fileServerButton).setOnClickListener(v -> {
            final Intent intent = new Intent(MainActivity.this, FileServerActivity.class);
            MainActivity.this.startActivity(intent);
        });

        findViewById(R.id.deleteAllButton).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            } else {
                deleteDownloadedFiles();
            }
        });

        ViewGroup container = ((ViewGroup) findViewById(R.id.singleDemoButton).getParent());
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView button = (TextView) container.getChildAt(i);
            button.setAllCaps(false);
        }
    }

    private void deleteDownloadedFiles() {
        final String[] namespaces = new String[]{
                DownloadListActivity.FETCH_NAMESPACE,
                FailedMultiEnqueueActivity.FETCH_NAMESPACE,
                FileServerActivity.FETCH_NAMESPACE};
        for (String namespace : namespaces) {
            final FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this).setNamespace(namespace).build();
            Fetch.Impl.getInstance(fetchConfiguration).deleteAll().close();
        }
        Fetch.Impl.getDefaultInstance().deleteAll().close();
        final RxFetch rxFetch = RxFetch.Impl.getDefaultRxInstance();
        rxFetch.deleteAll();
        rxFetch.close();
        new FetchFileServer.Builder(this)
                .setFileServerDatabaseName(FileServerActivity.FETCH_NAMESPACE)
                .setClearDatabaseOnShutdown(true)
                .build()
                .shutDown(false);
        try {
            final File fetchDir = new File(Data.getSaveDir(this));
            Utils.deleteFileAndContents(fetchDir);
            Toast.makeText(MainActivity.this, R.string.downloaded_files_deleted, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            deleteDownloadedFiles();
        } else {
            Snackbar.make(mainView, R.string.permission_not_enabled, Snackbar.LENGTH_INDEFINITE).show();
        }
    }
}
