package com.tonyodev.fetch2

/**
 * Action used by Fetch when enqueuing a request and a previous request with the
 * same file is already being managed. Default set on Request is EnqueueAction.REPLACE_EXISTING
 * which will replaces the existing request.
 * */
enum class EnqueueAction(val value: Int) {

    /** Replaces a previous managed request.*/
    REPLACE_EXISTING(0),

    /** Appends a numeric number to the file name and increments it accordingly
     * Example: text.txt, text(1).txt, text(2).txt.*/
    INCREMENT_FILE_NAME(1);


    companion object {

        @JvmStatic
        fun valueOf(value: Int): EnqueueAction {
            return when (value) {
                1 -> INCREMENT_FILE_NAME
                else -> REPLACE_EXISTING
            }
        }
    }
}