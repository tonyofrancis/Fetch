package com.tonyodev.fetch2core;

import org.jetbrains.annotations.Nullable;

/**
 * Callback interface used by Fetch to return
 * a potential null result to the caller.
 */
@FunctionalInterface
public interface Func2<R> {

    /**
     * Method called by Fetch to return requested information back to the caller.
     *
     * @param result Result of a request made by a caller. Result maybe null.
     */
    void call(@Nullable R result);

}