package com.tonyodev.fetch2.listener;

import android.support.annotation.NonNull;

import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.download.DownloadListener;

public interface FetchListener extends DownloadListener {
    void onAttach(@NonNull Fetch fetch);
    void onDetach(@NonNull Fetch fetch);
}
