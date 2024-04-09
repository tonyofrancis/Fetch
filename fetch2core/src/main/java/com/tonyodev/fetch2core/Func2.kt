package com.tonyodev.fetch2core

/**
 * Callback interface used by Fetch to return
 * a potential null result to the caller.
 */
fun interface Func2<R> {
    /**
     * Method called by Fetch to return requested information back to the caller.
     *
     * @param result Result of a request made by a caller. Result maybe null.
     */
    fun call(result: R?)
}