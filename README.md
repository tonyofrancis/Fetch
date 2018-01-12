[![Build Status](https://travis-ci.org/tonyofrancis/Fetch.svg?branch=v2)](https://travis-ci.org/tonyofrancis/Fetch)
[ ![Download](https://api.bintray.com/packages/tonyofrancis/maven/fetch2/images/download.svg?version=2.0.0-RC4) ](https://bintray.com/tonyofrancis/maven/fetch2/2.0.0-RC4/link)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Android%20Networking-blue.svg?style=flat)](https://android-arsenal.com/details/1/5196)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/tonyofrancis/Fetch/blob/master/LICENSE)

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
* Network specific downloading support.
* Ability to retry failed downloads.
* Ability to group downloads.
* Easy progress and status tracking.
* Download remaining time reporting (ETA).
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

How to use Fetch
----------------

Using Fetch is easy! Just add the Gradle dependency to your application's build.gradle file.

```java
implementation "com.tonyodev.fetch2:fetch2:2.0.0-RC4"
```

Next, get an instance of Fetch using the builder, and request a download.

```java
public class MainActivity extends AppCompatActivity {

    private Fetch mainFetch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainFetch = new Fetch.Builder(context, "Main")
                  .setDownloadConcurrentLimit(4) // Allows Fetch to download 4 downloads in Parallel.
                  .enableLogging(true)
                  .build();

        //Single enqueuing example            
        final Request request = new Request(url, file);
        request.setPriority(Priority.HIGH);
        request.setNetworkType(NetworkType.WIFI_ONLY);
        request.addHeader("clientKey", "SD78DF93_3947&MVNGHE1WONG");

        mainFetch.enqueue(request, new Func<Download>() {
               @Override
               public void call(Download download) {
                   //Request successfully Queued for download
               }
           }, new Func<Error>() {
               @Override
               public void call(Error error) {
                   //An error occurred when enqueuing a request.
               }
           });

        // Multi enqueuing example with group id   
        final List<Request> requestList = getSampleRequests();
        final int groupId = "MySampleGroup".hashCode();
        for(int i = 0; i < requestList.size(); i++) {
            requestList.get(i).setGroupId(groupId);
        }

        mainFetch.enqueue(requestList, new Func<List<? extends Download>>() {
            @Override
            public void call(List<? extends Download> downloads) {
                //Successfully enqueued requests.
            }
        }, new Func<Error>() {
            @Override
            public void call(Error error) {
                // An error occurred when enqueuing requests.
            }
        });
}
```

Tracking a download's progress and status is very easy with Fetch. Simply add a FetchListener to your Fetch instance, and the listener will be notified whenever a download's
status or progress changes.

```java

fetch.addListener(new FetchListener() {
    @Override
    public void onQueued(@NotNull Download download) {
        if (request.getId() == download.getId()) {
            showDownloadInList(download);
        }
    }

    @Override
    public void onCompleted(@NotNull Download download) {

    }

    @Override
    public void onError(@NotNull Download download) {

    }

    @Override
    public void onProgress(@NotNull Download download, long etaInMilliSeconds) {
      if (request.getId() == download.getId()) {
          updateDownload(download, etaInMilliSeconds);
      }
      final int progress = download.getProgress();
      Log.d("Fetch", "Progress Completed :" + progress);
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
});
```

Fetch supports pausing and resuming downloads using the request's id.
A request's id is a unique identifier that maps a request to a Fetch Download.
A download returned by Fetch will have have an id that matches the request id that
started the download.

```java

final Request request1 = new Request(url, file);
final Request request2 = new Request(url2, file2);

fetch.pause(request.getId());

...

fetch.resume(request2.getId());


...

//You can also pause and resume downloads using the group id the download belongs to.
int groupId = "AGroup".hashCode();
fetch.pauseGroup(groupId);
fetch.resumeGroup(groupId);

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
final int groupId = 52687447745;
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
to use the OkHttp Downloader instead. You can create your own custom downloaders
if necessary. See the Java docs for details.

```java
implementation "com.tonyodev.fetch2downloaders:fetch2downloaders:2.0.0-RC4"
```
Set the OkHttp Downloader for Fetch to use.
```java
final OkHttpClient okHttpClient = new OkHttpClient.Builder()
          .build();

final Fetch fetch = new Fetch.Builder(context, "Main")
	.setDownloader(new OkHttpDownloader(okHttpClient))
        .setDownloadConcurrentLimit(4) // Allows Fetch to download 4 downloads in Parallel.
        .enableLogging(true)
        .build();
```

RxFetch
----------------

If you would like to take advantage of RxJava2 features when using Fetch,
add the following gradle dependency to your application's build.gradle file.

```java
implementation "com.tonyodev.fetch2rx:fetch2rx:2.0.0-RC4"
```

RxFetch makes it super easy to enqueue download requests and query downloads using rxJava2 functional methods.

```java
final RxFetch rxFetch = new RxFetch.Builder(context, "Main")
  .setDownloadConcurrentLimit(2)
  .enableLogging(true)
  .build();

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

Fetch1 Migration
----------------

Migrate downloads from Fetch1 to Fetch2 using the migration assistant. Add the following gradle dependency to your application's build.gradle file.
```java
implementation "com.tonyodev.fetchmigrator:fetchmigrator:2.0.0-RC4"
```

Then run the Migrator.

```java
try {
    final String fetch2Namespace = "Main";
    FetchMigrator.migrateFromV1toV2(this, fetch2Namespace);
    FetchMigrator.deleteFetchV1Database(this);

    final Fetch fetch = new Fetch.Builder(context, fetch2Namespace)
      .setDownloadConcurrentLimit(4) // Allows Fetch to download 4 downloads in Parallel.
      .enableLogging(true)
      .build();

    fetch.getDownloads(new Func<List<? extends Download>>() {
        @Override
        public void call(List<? extends Download> downloads) {
            /* All downloads from the first version of Fetch
             * will now be maintained, downloaded and monitored in version 2 of Fetch.
            */
        }
    });
} catch (SQLException e) {
    e.printStackTrace();
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
