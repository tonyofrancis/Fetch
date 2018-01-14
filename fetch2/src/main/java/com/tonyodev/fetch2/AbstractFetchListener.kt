package com.tonyodev.fetch2

/**
 * An abstract FetchListener used by Fetch to report the different statuses and changes of the downloads
 * managed by Fetch.
 * @see com.tonyodev.fetch2.Status
 * */
abstract class AbstractFetchListener : FetchListener {

    /** Called when a new download is queued for download. The status of the download will be
     * Status.QUEUED.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * */
    override fun onQueued(download: Download) {

    }

    /** Called when a download completes. The status of the download will be Status.COMPLETED.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * */
    override fun onCompleted(download: Download) {

    }

    /** Called when an error occurs when downloading a download. The status of the download will be
     * Status.FAILED. See the download error field on the download for more information
     * on the specific error that occurred.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * */
    override fun onError(download: Download) {

    }

    /** Called several times to report the progress of a download when downloading.
     * The status of the download will be Status.DOWNLOADING.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     * Can return -1 to indicate that the estimated time remaining is unknown.
     * */
    override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {

    }

    /** Called when a download is paused. The status of the download will be Status.PAUSED.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * */
    override fun onPaused(download: Download) {

    }

    /** Called when a download is un-paused and queued again for download.
     * The status of the download will be Status.QUEUED.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * */
    override fun onResumed(download: Download) {

    }

    /** Called when a download is cancelled. The status of the download will be
     * Status.CANCELLED. The file for this download is not deleted.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * */
    override fun onCancelled(download: Download) {

    }

    /** Called when a download is removed and is no longer managed by Fetch or
     * contained in the Fetch database. The file for this download is not deleted.
     * The status of a download will be Status.REMOVED.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * */
    override fun onRemoved(download: Download) {

    }

    /** Called when a download is deleted and is no longer managed by Fetch or contained in
     * the fetch database. The downloaded file is deleted. The status of a download will be
     * Status.DELETED.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * */
    override fun onDeleted(download: Download) {

    }

}