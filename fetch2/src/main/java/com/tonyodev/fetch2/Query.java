package com.tonyodev.fetch2;

import android.support.annotation.Nullable;

public interface Query<T> {
    void onResult(@Nullable T result);
}
