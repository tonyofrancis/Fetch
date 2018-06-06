package com.tonyodev.fetch2core

import android.util.Log

/**
 * The default Fetch Logger.
 * */
open class FetchLogger(loggingEnabled: Boolean, loggingTag: String) : Logger {

    constructor() : this(DEFAULT_LOGGING_ENABLED, DEFAULT_TAG)

    /** Enable or disable logging.*/
    override var enabled: Boolean = loggingEnabled

    /** Sets the logging tag name. If the tag
     * name is more than 23 characters the default
     * tag name will be used as the tag.*/
    var tag: String = loggingTag

    private val loggingTag: String
        get() {
            return if (tag.length > 23) {
                DEFAULT_TAG
            } else {
                tag
            }
        }

    override fun d(message: String) {
        if (enabled) {
            Log.d(loggingTag, message)
        }
    }

    override fun d(message: String, throwable: Throwable) {
        if (enabled) {
            Log.d(loggingTag, message, throwable)
        }
    }

    override fun e(message: String) {
        if (enabled) {
            Log.e(loggingTag, message)
        }
    }

    override fun e(message: String, throwable: Throwable) {
        if (enabled) {
            Log.e(loggingTag, message, throwable)
        }
    }

}