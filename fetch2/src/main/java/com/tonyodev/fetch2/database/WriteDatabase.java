package com.tonyodev.fetch2.database;

import java.io.Closeable;
import java.util.List;

/**
 * Created by tonyofrancis on 6/29/17.
 */

public interface WriteDatabase extends Closeable {
    boolean remove(final long id);
    boolean insert(DatabaseRow databaseRow);
    void insert(List<DatabaseRow> databaseRows);
    void removeAll();
    void remove(long[] ids);
    void updateDownloadedBytes(final long id, final long downloadedBytes);
    void setDownloadedBytesAndTotalBytes(final long id, final long downloadedBytes, final long totalBytes);
    void setStatusAndError(final long id, final int status, final int error);
}
