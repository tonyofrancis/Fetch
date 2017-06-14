package com.tonyodev.fetch2sample;

import android.net.Uri;
import android.os.Environment;

import com.tonyodev.fetch2.Request;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by tonyofrancis on 1/24/17.
 */

public final class Data {

    public static final String[] sampleUrls = new String[] {
            "http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_640x360.m4v",
            "http://media.mongodb.org/zips.json",
            "http://www.example/some/unknown/123/Errorlink.txt",
            "http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_640x360.m4v",
            "http://storage.googleapis.com/ix_choosemuse/uploads/2016/02/android-logo.png"};

    private Data() {
    }

    static List<Request> getFetchRequests() {

        List<Request> requests = new ArrayList<>();

        for (String sampleUrl : sampleUrls) {

            Request request = new Request(sampleUrl,getFilePath(sampleUrl));
            requests.add(request);
        }

        return requests;
    }

    public static String getFilePath(String url) {

        Uri uri = Uri.parse(url);

        String fileName = uri.getLastPathSegment();

        String dir = getSaveDir();

        return (dir + "/DownloadList/" + System.nanoTime() +"_"+ fileName);
    }

    public static List<Request> getGameUpdates() {

        List<Request> requests = new ArrayList<>();
        String url = "http://speedtest.ftp.otenet.gr/files/test100k.db";

        for (int i = 0; i < 10; i++) {

            String filePath = getSaveDir() + "/gameAssets/" + "asset_" + i + ".asset";

            Request request = new Request(url,filePath);

            requests.add(request);
        }

        return requests;
    }

    public static String getSaveDir() {

        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/fetch";
    }
}
