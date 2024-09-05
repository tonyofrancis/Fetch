package com.tonyodev.fetchapp;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import androidx.annotation.NonNull;

import com.tonyodev.fetch2.Priority;
import com.tonyodev.fetch2.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class Data {

    public static final String[] sampleUrls = new String[]{
            "https://gist.githubusercontent.com/gcollazo/884a489a50aec7b53765405f40c6fbd1/raw/49d1568c34090587ac82e80612a9c350108b62c5/sample.json",
            "https://download.blender.org/peach/bigbuckbunny_movies/big_buck_bunny_720p_stereo.avi",
            "https://media.mongodb.org/zips.json",
            "http://www.exampletonyotest/some/unknown/123/Errorlink.txt",
            "https://upload.wikimedia.org/wikipedia/commons/6/64/Android_logo_2019_%28stacked%29.svg",
            "https://upload.wikimedia.org/wikipedia/commons/6/66/Android_robot.png",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"};

    private Data() {

    }

    @NonNull
    private static List<Request> getFetchRequests(Context context) {
        final List<Request> requests = new ArrayList<>();
        for (String sampleUrl : sampleUrls) {
            final Request request = new Request(sampleUrl, getFilePath(sampleUrl, context));
            requests.add(request);
        }
        return requests;
    }

    @NonNull
    public static List<Request> getFetchRequestWithGroupId(final int groupId, Context context) {
        final List<Request> requests = getFetchRequests(context);
        for (Request request : requests) {
            request.setGroupId(groupId);
        }
        return requests;
    }

    @NonNull
    private static String getFilePath(@NonNull final String url, Context context) {
        final Uri uri = Uri.parse(url);
        final String fileName = uri.getLastPathSegment();
        final String dir = getSaveDir(context);
        return (dir + "/DownloadList/" + fileName);
    }

    @NonNull
    static String getNameFromUrl(final String url) {
        return Objects.requireNonNull(Uri.parse(url).getLastPathSegment());
    }

    @NonNull
    public static List<Request> getGameUpdates(Context context) {
        final List<Request> requests = new ArrayList<>();
        final String url = "https://gist.githubusercontent.com/gcollazo/884a489a50aec7b53765405f40c6fbd1/raw/49d1568c34090587ac82e80612a9c350108b62c5/sample.json";
        for (int i = 0; i < 3; i++) {
            final String filePath = getSaveDir(context) + "/gameAssets/" + "asset_" + i + ".asset";
            final Request request = new Request(url, filePath);
            request.setPriority(Priority.HIGH);
            requests.add(request);
        }
        return requests;
    }

    @NonNull
    public static String getSaveDir(Context context) {
        return Objects.requireNonNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)) + "/fetch";
    }

}
