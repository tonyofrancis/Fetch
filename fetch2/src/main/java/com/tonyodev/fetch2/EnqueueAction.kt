package com.tonyodev.fetch2

/**
 * Action used by Fetch when enqueuing a request and a previous request with the
 * same file is already being managed by Fetch. The default set on a Request is EnqueueAction.UPDATE_ACCORDINGLY
 * which will replaces the existing request.
 * */
enum class EnqueueAction(val value: Int) {

    /** Replaces a previous managed request/download.*/
    REPLACE_EXISTING(0),

    /** Appends a numeric value to the file name and increments it accordingly
     * Example: text.txt, text(1).txt, text(2).txt.
     * Does not work with requests that provides a file uri that points to a content provider,
     * storage access framework, or anything other that an absolute file path.
     * */
    INCREMENT_FILE_NAME(1),

    /** Fetch will not enqueue the new request if a request already managed by Fetch has the same file path. */
    DO_NOT_ENQUEUE_IF_EXISTING(2),

    /** If Fetch is already managing an existing request with the same file path do one the following:
     * 1: If existing download is completed, Fetch will call onComplete method on attached Fetch Listeners.
     * 2: If existing download is not completed, resume download normally.
     * 3: If not downloaded, download will proceed normally.
     * Note: If download is existing, Fetch will update the old request/download with the new settings on
     * from the request object.
     * */
    UPDATE_ACCORDINGLY(3);

    companion object {

        @JvmStatic
        fun valueOf(value: Int): EnqueueAction {
            return when (value) {
                1 -> INCREMENT_FILE_NAME
                2 -> DO_NOT_ENQUEUE_IF_EXISTING
                3 -> UPDATE_ACCORDINGLY
                else -> REPLACE_EXISTING
            }
        }
    }

}