package com.tonyodev.fetch2

/**
 * Callback interface used by Fetch to return
 * a potential null result to the caller.
 * */
@FunctionalInterface
interface Func2<in T> {

    /**
     * Method called by Fetch to return requested information back to the caller.
     * @param t Results of a request made by a caller. Maybe null.
     * */
    fun call(t: T?)

}