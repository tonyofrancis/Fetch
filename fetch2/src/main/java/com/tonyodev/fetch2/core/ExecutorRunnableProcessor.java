package com.tonyodev.fetch2.core;

import android.support.annotation.NonNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by tonyofrancis on 6/28/17.
 */

public final class ExecutorRunnableProcessor implements RunnableProcessor {

    private final ConcurrentLinkedQueue<Runnable> queue;
    private final ExecutorService executor;

    public ExecutorRunnableProcessor() {
        queue = new ConcurrentLinkedQueue<>();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public synchronized void queue(@NonNull Runnable action) {
        boolean wasEmpty = queue.isEmpty();
        queue.add(action);
        if (wasEmpty) {
            next();
        }
    }

    @Override
    public synchronized void next() {
        if(!queue.isEmpty()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Runnable runnable = queue.poll();
                    if (runnable != null) {
                        runnable.run();
                        next();
                    }
                }
            });
        }
    }

    @Override
    public void clear() {
        queue.clear();
    }
}
