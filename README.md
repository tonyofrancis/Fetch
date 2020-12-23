
[![Build Status](https://travis-ci.org/tonyofrancis/Fetch.svg?branch=v2)](https://travis-ci.org/tonyofrancis/Fetch)
[ ![Download](https://api.bintray.com/packages/tonyofrancis/maven/fetch2/images/download.svg?version=3.0.12) ](https://bintray.com/tonyofrancis/maven/fetch2/3.0.12/link)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Networking-blue.svg?style=flat)](https://android-arsenal.com/details/1/5196)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/tonyofrancis/Fetch/blob/master/LICENSE)

![ScreenShot](https://github.com/tonyofrancis/Fetch/blob/v2/full_logo.png)

Overview
--------

Fetch is a simple, powerful, customizable file download manager library for Android.

![ScreenShot](https://github.com/tonyofrancis/Fetch/blob/v2/screenshot.png)

Features
--------

* Simple and easy to use API.
* Continuous downloading in the background.
* Concurrent downloading support.
* Ability to pause and resume downloads.
* Set the priority of a download.
* Network-specific downloading support.
* Ability to retry failed downloads.
* Ability to group downloads.
* Easy progress and status tracking.
* Download remaining time reporting (ETA).
* Download speed reporting.
* Save and Retrieve download information anytime.
* Notification Support.
* Storage Access Framework, Content Provider and URI support.
* And more...

Prerequisites
-------------

If you are saving downloads outside of your application's sandbox, you will need to
add the following storage permissions to your application's manifest. For Android SDK version
23(M) and above, you will also need to explicitly request these permissions from the user.

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```
Also, as you are going to use Internet to download files. We need to add the Internet access permissions
in the Manifest.

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

How to use Fetch
----------------

Using Fetch is easy! Just add the Gradle dependency to your application's build.gradle file.

```java
implementation "com.tonyodev.fetch2:fetch2:3.0.12"
```
Androidx use:
```java
implementation "androidx.tonyodev.fetch2:xfetch2:3.1.6"
```

Next, get an instance of Fetch and request a download.

```java
public class TestActivity extends AppCompatActivity {

    private Fetch fetch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

 FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setDownloadConcurrentLimit(3)
                .build();

        fetch = Fetch.Impl.getInstance(fetchConfiguration);

        String url = "http:www.example.com/test.txt";
        String file = "/downloads/test.txt";
        
        final Request request = new Request(url, file);
        request.setPriority(Priority.HIGH);
        request.setNetworkType(NetworkType.ALL);
        request.addHeader("clientKey", "SD78DF93_3947&MVNGHE1WONG");
        
        fetch.enqueue(request, updatedRequest -> {
            //Request was successfully enqueued for download.
        }, error -> {
            //An error occurred enqueuing the request.
        });

    }
}
```

Tracking a download's progress and status is very easy with Fetch. 
Simply add a FetchListener to your Fetch instance, and the listener will be notified whenever a download's
status or progress changes.

```java
FetchListener fetchListener = new FetchListener() {
    @Override
    public void onQueued(@NotNull Download download, boolean waitingOnNetwork) {
        if (request.getId() == download.getId()) {
            showDownloadInList(download);
        }
    }

    @Override
    public void onCompleted(@NotNull Download download) {

    }

    @Override
    public void onError(@NotNull Download download) {
        Error error = download.getError();
    }

    @Override
    public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
        if (request.getId() == download.getId()) {
            updateDownload(download, etaInMilliSeconds);
        }
        int progress = download.getProgress();
    }

    @Override
    public void onPaused(@NotNull Download download) {

    }

    @Override
    public void onResumed(@NotNull Download download) {

    }

    @Override
    public void onCancelled(@NotNull Download download) {

    }

    @Override
    public void onRemoved(@NotNull Download download) {

    }

    @Override
    public void onDeleted(@NotNull Download download) {

    }
};

fetch.addListener(fetchListener);

//Remove listener when done.
fetch.removeListener(fetchListener);
```

Fetch supports pausing and resuming downloads using the request's id.
A request's id is a unique identifier that maps a request to a Fetch Download.
A download returned by Fetch will have have an id that matches the request id that
started the download.

```java
Request request1 = new Request(url, file);
Request request2 = new Request(url2, file2);

fetch.pause(request1.getId());

...

fetch.resume(request2.getId());

```

You can query Fetch for download information in several ways.

```java
//Query all downloads
fetch.getDownloads(new Func<List<? extends Download>>() {
    @Override
        public void call(List<? extends Download> downloads) {
            //Access all downloads here
        }
});

//Get all downloads with a status
fetch.getDownloadsWithStatus(Status.DOWNLOADING, new Func<List<? extends Download>>() {
    @Override
        public void call(List<? extends Download> downloads) {
            //Access downloads that are downloading
        }
});

// You can also access grouped downloads
int groupId = 52687447745;
fetch.getDownloadsInGroup(groupId, new Func<List<? extends Download>>() {
    @Override
      public void call(List<? extends Download> downloads) {
              //Access grouped downloads
      }
});
```

When you are done with an instance of Fetch, simply release it.

```java
//do work

fetch.close();

//do more work
```

Downloaders
----------------

By default Fetch uses the HttpUrlConnection client via the HttpUrlConnectionDownloader
to download requests. Add the following Gradle dependency to your application's build.gradle
to use the OkHttp Downloader instead. You can create your custom downloaders
if necessary. See the Java docs for details.

```java
implementation "com.tonyodev.fetch2okhttp:fetch2okhttp:3.0.12"
```
Androidx use:
```java
implementation "androidx.tonyodev.fetch2okhttp:xfetch2okhttp:3.1.6"
```

Set the OkHttp Downloader for Fetch to use.
```java
OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
    .setDownloadConcurrentLimit(10)
    .setHttpDownloader(new OkHttpDownloader(okHttpClient))
    .build();

Fetch fetch = Fetch.Impl.getInstance(fetchConfiguration);
```

RxFetch
----------------

If you would like to take advantage of RxJava2 features when using Fetch,
add the following gradle dependency to your application's build.gradle file.

```java
implementation "com.tonyodev.fetch2rx:fetch2rx:3.0.12"
```
Androidx use:
```java
implementation "androidx.tonyodev.fetch2rx:xfetch2rx:3.1.6"
```

RxFetch makes it super easy to enqueue download requests and query downloads using rxJava2 functional methods.

```java
FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this).build();
Rxfetch rxFetch = RxFetch.Impl.getInstance(fetchConfiguration);

rxFetch.getDownloads()
        .asFlowable()
        .subscribe(new Consumer<List<Download>>() {
            @Override
            public void accept(List<Download> downloads) throws Exception {
                //Access results
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                //An error occurred
                final Error error = FetchErrorUtils.getErrorFromThrowable(throwable);
            }
        });
```

FetchFileServer
----------------

Introducing the FetchFileServer. The FetchFileServer is a lightweight TCP File Server that acts like
an HTTP file server designed specifically to share files between Android devices. You can host file resources
with the FetchFileServer on one device and have to Fetch download Files from the server
on another device. See the sample app for more information. Wiki on FetchFileServer will be
added in the coming days.

Start using FetchFileServer by adding the gradle dependency to your application's build.gradle file.
```java
implementation "com.tonyodev.fetch2fileserver:fetch2fileserver:3.0.12"
```
Androidx use: 
```java
implementation "androidx.tonyodev.fetch2fileserver:xfetch2fileserver:3.1.6"
```

Start a FetchFileServer instance and add resource files that it can serve to connected clients.
```java
public class TestActivity extends AppCompatActivity {

    FetchFileServer fetchFileServer;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fetchFileServer = new FetchFileServer.Builder(this)
                .build();
        
        fetchFileServer.start(); //listen for client connections

        File file = new File("/downloads/testfile.txt");
        FileResource fileResource = new FileResource();
        fileResource.setFile(file.getAbsolutePath());
        fileResource.setLength(file.length());
        fileResource.setName("testfile.txt");
        fileResource.setId(UUID.randomUUID().hashCode());
        
        fetchFileServer.addFileResource(fileResource);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fetchFileServer.shutDown(false);
    }
}
```

Downloading a file from a FetchFileServer using the Fetch is easy.

```java
public class TestActivity extends AppCompatActivity {

    Fetch fetch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(this)
                .setFileServerDownloader(new FetchFileServerDownloader()) //have to set the file server downloader
                .build();
        fetch = Fetch.Impl.getInstance(fetchConfiguration);
        fetch.addListener(fetchListener);

        String file = "/downloads/sample.txt";
        String url = new FetchFileServerUrlBuilder()
                .setHostInetAddress("127.0.0.1", 6886) //file server ip and port
                .setFileResourceIdentifier("testfile.txt") //file resource name or id
                .create();
        Request request = new Request(url, file);
        fetch.enqueue(request, request1 -> {
            //Request enqueued for download
        }, error -> {
            //Error while enqueuing download
        });
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
    }

    private FetchListener fetchListener = new AbstractFetchListener() {
        @Override
        public void onProgress(@NotNull Download download, long etaInMilliSeconds, long downloadedBytesPerSecond) {
            super.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond);
            Log.d("TestActivity", "Progress: " + download.getProgress());
        }

        @Override
        public void onError(@NotNull Download download) {
            super.onError(download);
            Log.d("TestActivity", "Error: " + download.getError().toString());
        }

        @Override
        public void onCompleted(@NotNull Download download) {
            super.onCompleted(download);
            Log.d("TestActivity", "Completed ");
        }
    };
}
```

Fetch1 Migration
----------------

Migrate downloads from Fetch1 to Fetch2 using the migration assistant. Add the following gradle dependency to your application's build.gradle file.
```java
implementation "com.tonyodev.fetchmigrator:fetchmigrator:3.0.12"
```
Androidx use:
```java
implementation "androidx.tonyodev.fetchmigrator:xfetchmigrator:3.1.6"
```

Then run the Migrator.

```java
        if (!didMigrationRun()) {
            //Migration has to run on a background thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final List<DownloadTransferPair> transferredDownloads = FetchMigrator.migrateFromV1toV2(getApplicationContext(), APP_FETCH_NAMESPACE);
                        //TODO: update external references of ids
                        for (DownloadTransferPair transferredDownload : transferredDownloads) {
                            Log.d("newDownload", transferredDownload.getNewDownload().toString());
                            Log.d("oldId in Fetch v1", transferredDownload.getOldID() + "");
                        }
                        FetchMigrator.deleteFetchV1Database(getApplicationContext());
                        setMigrationDidRun(true);
                        //Setup and Run Fetch2 after the migration
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            //Setup and Run Fetch2  normally
        }
```

Contribute
----------

Fetch can only get better if you make code contributions. Found a bug? Report it.
Have a feature idea you'd love to see in Fetch? Contribute to the project!

License
-------

```
Copyright (C) 2017 Tonyo Francis.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
