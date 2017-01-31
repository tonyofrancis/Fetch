package com.tonyodev.fetchdemo;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import java.io.File;

/**
 * Created by tonyofrancis on 1/25/17.
 */

public final class Utils {

    private Utils() {
    }

    public static String getMimeType(Context context,@NonNull Uri uri) {

        ContentResolver cR = context.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String type = mime.getExtensionFromMimeType(cR.getType(uri));

        if(type == null) {
            type = "*/*";
        }

        return type;
    }

    public static void deleteFileAndContents(File file) throws Exception {

        if(file == null) {
            return;
        }

        if(file.exists()) {

            if(file.isDirectory()) {

                File[] contents = file.listFiles();

                if(contents != null) {

                    for (File content : contents) {
                        deleteFileAndContents(content);
                    }
                }
            }

            file.delete();
        }
    }
}
