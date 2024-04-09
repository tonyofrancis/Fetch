package com.tonyodev.fetch2core;

import org.jetbrains.annotations.NotNull;


/**
 * Callback interface used by Fetch to return
 * results to the caller.
 */
@FunctionalInterface
public interface Func<R> {

    /**
     * Method called by Fetch to return requested information back to the caller.
     *
     * @param result Result of a request made by a caller. Result is never null.
     */
    void call(@NotNull R result);

}