package com.tonyodev.fetch2core

/**
 * The reasons why onChanged method was called for a FetchObserver.
 * */
enum class Reason(val value: Int) {

    /** The reason was not specified.*/
    NOT_SPECIFIED(0),

    /* The download was added to Fetch database.*/
    DOWNLOAD_ADDED(1),

    /* The download was queued for download.*/
    DOWNLOAD_QUEUED(2),

    /* The download is has started.*/
    DOWNLOAD_STARTED(3),

    /** The download is waiting on network to resume downloading.*/
    DOWNLOAD_WAITING_ON_NETWORK(4),

    /** The download progress has changed.*/
    DOWNLOAD_PROGRESS_CHANGED(5),

    /** The download has completed.*/
    DOWNLOAD_COMPLETED(6),

    /** An error occurred during download. See the download.getError() method.*/
    DOWNLOAD_ERROR(7),

    /** The download has been paused.*/
    DOWNLOAD_PAUSED(8),

    /** The download has been resumed.*/
    DOWNLOAD_RESUMED(9),

    /** The download has been cancelled.*/
    DOWNLOAD_CANCELLED(10),

    /** The download has been removed.*/
    DOWNLOAD_REMOVED(11),

    /** The download has been deleted.*/
    DOWNLOAD_DELETED(12),

    /** The download block has been updated. This is not used by FetchObservers
     * because the download blocks are updated on background threads very frequently
     * and will bog down the UI thread. FetchObservers are only called on the UI thread.
     * If you need to listen for this event. Use the FetchListeners instead.
     * */
    DOWNLOAD_BLOCK_UPDATED(13),

    /** When the FetchObserver is attached for the first time.*/
    OBSERVER_ATTACHED(14),

    /** A normal reporting of a FetchObserver. Used to report updates. etc.*/
    REPORTING(15);

    companion object {

        @JvmStatic
        fun valueOf(value: Int): Reason {
            return when(value) {
                1 -> DOWNLOAD_ADDED
                2 -> DOWNLOAD_QUEUED
                3 -> DOWNLOAD_STARTED
                4 -> DOWNLOAD_WAITING_ON_NETWORK
                5 -> DOWNLOAD_PROGRESS_CHANGED
                6 -> DOWNLOAD_COMPLETED
                7 -> DOWNLOAD_ERROR
                8 -> DOWNLOAD_PAUSED
                9 -> DOWNLOAD_RESUMED
                10 -> DOWNLOAD_CANCELLED
                11 -> DOWNLOAD_REMOVED
                12 -> DOWNLOAD_DELETED
                13 -> DOWNLOAD_BLOCK_UPDATED
                14 -> OBSERVER_ATTACHED
                15 -> REPORTING
                else -> NOT_SPECIFIED
            }
        }

    }

}