package com.tonyodev.fetch2.core;

/**
 * Created by tonyofrancis on 6/11/17.
 */

public interface RunnableProcessor {
    void queue(Runnable runnable);
    void next();
    void clear();
}
