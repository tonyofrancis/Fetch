package com.tonyodev.fetchapp;

import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;

import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public final class Data {

    public static final String[] sampleUrls = new String[]{
            "http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_640x360.m4v",
            "http://media.mongodb.org/zips.json",
            "http://www.example/some/unknown/123/Errorlink.txt",
            "http://storage.googleapis.com/ix_choosemuse/uploads/2016/02/android-logo.png"
    };

    private Data() {

    }

    @NonNull
    public static List<Request> getFetchRequests() {
        final List<Request> requests = new ArrayList<>();
        for (String sampleUrl : sampleUrls) {
            final Request request = new Request(UUID.randomUUID().hashCode(), sampleUrl, getFilePath(sampleUrl));
            requests.add(request);
        }
        return requests;
    }

    @NonNull
    public static List<Request> getFetchRequestWithGroupId(final int groupId) {
        final List<Request> requests = getFetchRequests();
        for (Request request : requests) {
            request.setGroupId(groupId);
        }
        return requests;
    }

    @NonNull
    public static String getFilePath(@NonNull final String url) {
        final Uri uri = Uri.parse(url);
        final String fileName = uri.getLastPathSegment();
        final String dir = getSaveDir();
        return (dir + "/DownloadList/" + fileName);
    }

    @NonNull
    public static List<Request> getGameUpdates() {
        final List<Request> requests = new ArrayList<>();
        final String url = "http://speedtest.ftp.otenet.gr/files/test100k.db";
        for (int i = 0; i < 10; i++) {
            final String filePath = getSaveDir() + "/gameAssets/" + "asset_"
                    + i + ".asset";
            final Request request = new Request(url, filePath);
            request.setPriority(Priority.HIGH);
            requests.add(request);
        }
        return requests;
    }

    @NonNull
    public static String getSaveDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/fetch";
    }

}
