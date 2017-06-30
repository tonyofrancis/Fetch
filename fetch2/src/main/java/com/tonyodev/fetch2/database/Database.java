package com.tonyodev.fetch2.database;

/**
 * Created by tonyofrancis on 6/11/17.
 */

public interface Database extends ReadDatabase, WriteDatabase {
    void updateDownloadedBytes(final long id, final long downloadedBytes);
    void setDownloadedBytesAndTotalBytes(final long id, final long downloadedBytes, final long totalBytes);
    void setStatusAndError(final long id, final int status, final int error);
}
