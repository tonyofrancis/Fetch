package com.tonyodev.fetch2

/**
 * Action used by Fetch when enqueuing a request and a previous request with the
 * same file is already being managed by Fetch. The default set on a Request is EnqueueAction.REPLACE_EXISTING
 * which will replaces the existing request.
 * */
enum class EnqueueAction(val value: Int) {

    /** Replaces a previous managed request/download.*/
    REPLACE_EXISTING(0),

    /** Appends a numeric value to the file name and increments it accordingly
     * Example: text.txt, text(1).txt, text(2).txt.*/
    INCREMENT_FILE_NAME(1),

    /** Fetch will not enqueue the new request if a request already managed by Fetch has the same file path. */
    DO_NOT_ENQUEUE_IF_EXISTING(2);

    companion object {

        @JvmStatic
        fun valueOf(value: Int): EnqueueAction {
            return when (value) {
                1 -> INCREMENT_FILE_NAME
                2 -> DO_NOT_ENQUEUE_IF_EXISTING
                else -> REPLACE_EXISTING
            }
        }
    }

}