package com.tonyodev.fetchdemo;

/**
 * Created by tonyofrancis on 1/24/17.
 */

public interface ActionListener {

    void onPauseDownload(long id);
    void onResumeDownload(long id);
    void onRemoveDownload(long id);
    void onRetryDownload(long id);
}
