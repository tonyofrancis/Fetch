package com.tonyodev.fetchapp;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import timber.log.Timber;

public final class Utils {

    private Utils() {

    }

    @NonNull
    public static String getMimeType(@NonNull final Context context, @NonNull final Uri uri) {
        final ContentResolver cR = context.getContentResolver();
        final MimeTypeMap mime = MimeTypeMap.getSingleton();
        String type = mime.getExtensionFromMimeType(cR.getType(uri));
        if (type == null) {
            type = "*/*";
        }
        return type;
    }

    public static void deleteFileAndContents(@NonNull final File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                final File[] contents = file.listFiles();
                if (contents != null) {
                    for (final File content : contents) {
                        deleteFileAndContents(content);
                    }
                }
            }
            boolean result = file.delete();
            Timber.d("The result of the deletion of the (%s) file was (%s)", file.getName(), result);
        }
    }

    @NonNull
    public static String getETAString(@NonNull final Context context, final long etaInMilliSeconds) {
        if (etaInMilliSeconds < 0) {
            return "";
        }
        int seconds = (int) (etaInMilliSeconds / 1000);
        long hours = seconds / 3600;
        seconds -= (int) (hours * 3600);
        long minutes = seconds / 60;
        seconds -= (int) (minutes * 60);
        if (hours > 0) {
            return context.getString(R.string.download_eta_hrs, hours, minutes, seconds);
        } else if (minutes > 0) {
            return context.getString(R.string.download_eta_min, minutes, seconds);
        } else {
            return context.getString(R.string.download_eta_sec, seconds);
        }
    }

    @NonNull
    public static String getDownloadSpeedString(@NonNull final Context context, final long downloadedBytesPerSecond) {
        if (downloadedBytesPerSecond < 0) {
            return "";
        }
        double kb = (double) downloadedBytesPerSecond / (double) 1000;
        double mb = kb / (double) 1000;
        final DecimalFormat decimalFormat = new DecimalFormat(".##");
        if (mb >= 1) {
            return context.getString(R.string.download_speed_mb, decimalFormat.format(mb));
        } else if (kb >= 1) {
            return context.getString(R.string.download_speed_kb, decimalFormat.format(kb));
        } else {
            return context.getString(R.string.download_speed_bytes, downloadedBytesPerSecond);
        }
    }

    @NonNull
    public static File createFile(String filePath) {
        final File file = new File(filePath);
        if (!file.exists()) {
            final File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean result = parent.mkdirs();
                Timber.d("The result of the creation of the (%s) directory was (%s)", parent.getName(), result);
            }
            try {
                boolean result = file.createNewFile();
                Timber.d("The result of the creation of the (%s) file was (%s)", file.getName(), result);
            } catch (IOException e) {
                Timber.d(e, "Failed to create the file: %s", file.getAbsolutePath());
            }
        }
        return file;
    }

}
