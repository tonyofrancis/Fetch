package com.tonyodev.fetch2;

/**
 * Created by tonyofrancis on 6/11/17.
 */

interface ActionProcessor<T> {

    void queueAction(T action);

    void processNext();

    void clearQueue();
}
