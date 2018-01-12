package com.tonyodev.fetch2

/**
 * Used to create custom loggers for Fetch.
 * */
interface Logger {

    /** Enable or disable logging.*/
    var enabled: Boolean

    /** Log debug information.
     * @param message message
     * */
    fun d(message: String)

    /** Log debug information with throwable.
     * @param message message
     * @param throwable throwable
     * */
    fun d(message: String, throwable: Throwable)

    /** Log error information.
     * @param message message
     * */
    fun e(message: String)

    /** Log error information with throwable.
     * @param message message
     * @param throwable throwable
     * */
    fun e(message: String, throwable: Throwable)

}