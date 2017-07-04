package com.tonyodev.fetch2.callback;

import android.support.annotation.NonNull;

import com.tonyodev.fetch2.Error;
import com.tonyodev.fetch2.Request;

public interface Callback {
    void onQueued(@NonNull Request request);
    void onFailure(@NonNull Request request, @NonNull Error error);
}
