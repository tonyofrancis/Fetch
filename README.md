Fetch
=====

A better in app Download Manager for Android.

Overview
--------

Fetch is a simple yet powerful Android library that allows you to manage downloads
more efficiently in your Android apps. It uses a background service on the device 
to download and maintain requests.

Features
--------

* Simple and easy to use API.
* Continuous downloading in the background.
* Ability to pause and resume downloads.
* Set the priority of a download request.
* Ability to retry failed downloads.
* Easy progress and status tracking.
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

Using Fetch is easy! Just add the Gradle dependency to your application's build.gradle
file.

```java 
compile 'com.tonyodev.fetch:fetch:1.0.0'
``` 

Next, get an instance of Fetch and request a download. A unique ID will be returned 
for each request. 
```java

public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fetch fetch = Fetch.getInstance(this);
        
        ...
        
        Request request = new Request(url,filePath);
        long downloadId = fetch.enqueue(request);        
    }

}
```
Tracking a download's progress and status is very easy with Fetch. Simply add a FetchListener
to your instance, and the listener will be notified whenever a download's
status or progress changes. 

```java

fetch.addFetchListener(new FetchListener() {
            
    @Override
    public void onUpdate(long id, int status, int progress, int error) {
                
        if(downloadId == id) {

            progressBar.setProgress(progress);
            ...
        }
    }     
});
``` 
Fetch supports pausing and resuming downloads. If a download request fails, you can opt
to retry.
```java
...

fetch.pause(downloadId);

...

fetch.resume(downloadId);

...

fetch.retry(downloadId);
```

When you are done with an instance of Fetch, simply release it. **It is important
that you release instances of fetch to prevent memory leaks.**
```java
//do work

fetch.release();

//do more work
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