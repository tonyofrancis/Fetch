package com.tonyodev.fetch2;

import android.support.annotation.NonNull;

public interface Callback {
    void onQueued(@NonNull Request request);
    void onFailure(@NonNull Request request, @NonNull Error error);
}
