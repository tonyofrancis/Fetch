package com.tonyodev.fetch2.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by tonyofrancis on 6/24/17.
 */

public final class Utils {

    private Utils(){}

    public static long[] createIdArray(List<Long> ids) {

        for (Long id : ids) {
            if(id == null) {
                throw new NullPointerException("id inside List<Long> cannot be null");
            }
        }

        long[] idArray = new long[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            idArray[i] = ids.get(i);
        }

        return idArray;
    }

    public static File createFileOrThrow(String filePath) throws IOException {

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

    public static boolean createDirIfNotExist(String path) {
        File dir = new File(path);
        return dir.exists() || dir.mkdirs();
    }

    public static boolean createFileIfNotExist(String path) throws IOException {
        File file = new File(path);
        return file.exists() || file.createNewFile();
    }

    public static int calculateProgress(long downloadedBytes, long fileSize) {

        if (fileSize < 1 || downloadedBytes < 1) {
            return  0;
        } else if(downloadedBytes >= fileSize) {
            return 100;
        } else {
            return (int) (((double) downloadedBytes / (double) fileSize) * 100);
        }
    }
}


