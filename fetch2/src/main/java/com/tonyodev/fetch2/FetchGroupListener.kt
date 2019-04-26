package com.tonyodev.fetch2

import com.tonyodev.fetch2core.DownloadBlock

/**
 * Listener used by Fetch to report the different statuses and changes of the downloads
 * managed by Fetch. This listener's callbacks also returns the group information.
 * @see com.tonyodev.fetch2.Status
 * Note: Attaching a FetchGroupListener to a Fetch instance adds an additional overhead to keep the fetchGroup
 * information returned fresh with the latest details for downloads in the group. If are handling group information
 * manually or do not need group information on listener callbacks, use the FetchListener interface or the AbstractFetchListener class.
 * */
interface FetchGroupListener: FetchListener {

    /** Called when a new download is added to Fetch. The status of the download will be
     * Status.ADDED.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onAdded(groupId: Int, download: Download, fetchGroup: FetchGroup)

    /** Called when a new download is queued for download. The status of the download will be
     * Status.QUEUED.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param waitingOnNetwork Indicates that the download was queued because it is waiting on
     * the right network condition. For example: Waiting on internet access to be restored or
     * waiting for a Wifi connection.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onQueued(groupId: Int, download: Download, waitingNetwork: Boolean, fetchGroup: FetchGroup)

    /**
     * Called to report that the download process has started for a request. The status of the download
     * will be Status.DOWNLOADING.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * @param downloadBlocks list of download's downloading blocks information.
     * @param totalBlocks total downloading blocks for a download.
     * about a specific download managed by Fetch.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onStarted(groupId: Int, download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int, fetchGroup: FetchGroup)

    /** Called when a download completes. The status of the download will be Status.COMPLETED.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onCompleted(groupId: Int, download: Download, fetchGroup: FetchGroup)

    /** Called when a download is un-paused and queued again for download.
     * @param groupId the groupId
     * The status of the download will be Status.QUEUED.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onResumed(groupId: Int, download: Download, fetchGroup: FetchGroup)

    /** Called when a download is paused. The status of the download will be Status.PAUSED.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onPaused(groupId: Int, download: Download, fetchGroup: FetchGroup)

    /** Called when a download is cancelled. The status of the download will be
     * Status.CANCELLED. The file for this download is not deleted.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onCancelled(groupId: Int, download: Download, fetchGroup: FetchGroup)

    /** Called when a download is removed and is no longer managed by Fetch or
     * contained in the Fetch database. The file for this download is not deleted.
     * The status of a download will be Status.REMOVED.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onRemoved(groupId: Int, download: Download, fetchGroup: FetchGroup)

    /** Called when a download is deleted and is no longer managed by Fetch or contained in
     * the fetch database. The downloaded file is deleted. The status of a download will be
     * Status.DELETED.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onDeleted(groupId: Int, download: Download, fetchGroup: FetchGroup)

    /** Called when a download is queued and waiting for the right network conditions to start downloading.
     * The status of the download will be Status.QUEUED. Note this method is called several time on
     * a background thread.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onWaitingNetwork(groupId: Int, download: Download, fetchGroup: FetchGroup)

    /** Called when an error occurs when downloading a download. The status of the download will be
     * Status.FAILED. See the download error field on the download for more information
     * on the specific error that occurred.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param error the error that occurred
     * @param throwable the throwable that caused the error to occur. Maybe null.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onError(groupId: Int, download: Download, error: Error, throwable: Throwable?, fetchGroup: FetchGroup)

    /** Called several times to report the progress of a download block belonging to a download.
     * The status of the download will be Status.DOWNLOADING. A download may be downloaded using
     * several downloading blocks if using the Parallel File Downloader. The Sequential Downloader
     * only uses 1 downloading block. See Downloader class documentation for more information.
     * Note: This method is called on a background thread.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param downloadBlock download's downloading block information.
     * @param totalBlocks total downloading blocks for a download.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onDownloadBlockUpdated(groupId: Int, download: Download, downloadBlock: DownloadBlock, totalBlocks: Int, fetchGroup: FetchGroup)

    /** Called several times to report the progress of a download when downloading.
     * The status of the download will be Status.DOWNLOADING.
     * @param groupId the groupId
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     * Can return -1 to indicate that the estimated time remaining is unknown.
     * @param fetchGroup An immutable object which contains a current snapshot of all the information
     * about a specific download group managed by Fetch.
     * */
    fun onProgress(groupId: Int, download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long, fetchGroup: FetchGroup)

}