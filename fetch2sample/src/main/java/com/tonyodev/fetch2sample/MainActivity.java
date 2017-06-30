package com.tonyodev.fetch2sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.tonyodev.fetch2.Fetch;

public class MainActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    ,STORAGE_PERMISSION_CODE);
        }

        findViewById(R.id.singleDemoButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this,SingleDownloadActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.downloadListButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this,DownloadListActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.gameFilesButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this,GameFilesActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.multiEnqueueButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this,MultiEnqueueActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.multiFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this,FragmentActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.deleteAllButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fetch.getInstance().deleteAll();
                Toast.makeText(MainActivity.this,R.string.downloaded_files_deleted,Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == STORAGE_PERMISSION_CODE || grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            //DO NOTHING STORAGE PERMISSION GRANTED
        }else {
            Toast.makeText(this, R.string.permission_not_enabled,Toast.LENGTH_LONG).show();
        }
    }
}
