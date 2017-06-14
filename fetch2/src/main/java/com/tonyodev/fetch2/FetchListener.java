package com.tonyodev.fetch2;

import android.support.annotation.NonNull;

public interface FetchListener extends DownloadListener {
    void onAttach(@NonNull Fetch fetch);
    void onDetach(@NonNull Fetch fetch);
}
