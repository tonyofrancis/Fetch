package com.tonyodev.fetch2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by tonyofrancis on 6/11/17.
 */

interface Database {

    boolean contains(long id);
    boolean insert(final long id, final String url, final String absoluteFilePath);
    @NonNull List<RequestData> queryByStatus(int status);
    @Nullable RequestData query(final long id);
    @NonNull List<RequestData> query();
    @NonNull List<RequestData> query(long[] ids);
    void updateDownloadedBytes(final long id, final long downloadedBytes);
    void setDownloadedBytesAndTotalBytes(final long id, final long downloadedBytes, final long totalBytes);
    void remove(final long id);
    void setStatusAndError(final long id, final Status status, final int error);
}
