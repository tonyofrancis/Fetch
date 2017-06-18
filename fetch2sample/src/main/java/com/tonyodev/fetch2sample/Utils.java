package com.tonyodev.fetch2sample;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

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
}
