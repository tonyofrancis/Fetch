package com.tonyodev.fetch2;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


final class DownloadHelper {

    private DownloadHelper() {}

    static okhttp3.Request createHttpRequest(RequestData requestData) {

        File file = new File(requestData.getAbsoluteFilePath());
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();

        builder.url(requestData.getUrl());

        for (String key : requestData.getHeaders().keySet()) {
            builder.addHeader(key,requestData.getHeaders().get(key));
        }

        builder.addHeader("Range","bytes=" + file.length() + "-");
        return builder.build();
    }

    static int calculateProgress(long downloadedBytes, long fileSize) {

        if (fileSize < 1 || downloadedBytes < 1) {
            return  0;
        } else if(downloadedBytes >= fileSize) {
            return 100;
        } else {
            return (int) (((double) downloadedBytes / (double) fileSize) * 100);
        }
    }

    static boolean hasTwoSecondsPassed(long startTime, long stopTime) {
        return TimeUnit.NANOSECONDS.toSeconds(stopTime - startTime) >= 2;
    }

    static File createFileOrThrow(String filePath) throws IOException {

        File file = new File(filePath);

        if(file.exists()) {
            return file;
        }

        boolean parentDirCreated = createDirIfNotExist(file.getParentFile().getAbsolutePath());
        boolean fileCreated = createFileIfNotExist(file.getAbsolutePath());

        if(!parentDirCreated || !fileCreated) {
            throw new IOException("File could not be created for the filePath:" + filePath);
        }

        return new File(filePath);
    }

    static boolean createDirIfNotExist(String path) {
        File dir = new File(path);
        return dir.exists() || dir.mkdirs();
    }

    static boolean createFileIfNotExist(String path) throws IOException {
        File file = new File(path);
        return file.exists() || file.createNewFile();
    }

    static boolean canRetry(Status status) {
        switch (status) {
            case COMPLETED:
                return false;
            default:
                return true;
        }
    }

    static boolean canCancel(Status status) {
        switch (status) {
            case CANCELLED:
                return false;
            default:
                return true;
        }
    }
}
