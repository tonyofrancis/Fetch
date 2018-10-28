package com.tonyodev.fetch2

import android.app.Notification
import android.app.PendingIntent
import android.support.v4.app.NotificationCompat
import com.tonyodev.fetch2core.DownloadBlock
import java.io.Closeable

/**
 * Implement this interface to create, maintain and show notifications
 * for downloads.
 * */
interface NotificationManager : Closeable {

    /**
     * Created a new notification for the download. Called on a background notification thread.
     * @param download the download
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     *         -1 if the eta is unknown.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     *         -1 if the downloadedBytesPerSecond is unknown or not provided.
     * */
    fun updateNotificationBuilder(notificationBuilder: NotificationCompat.Builder, download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long)

    /**
     * Get a new PendingIntent associated with a notification action type.
     * Called on a background notification thread.
     * @param download the download
     * @param actionType the action the intent will perform when the user clicks the action
     *        on the notification. See the FetchIntent.kt file for the different action types.
     * @return the action pending intent or null.
     *
     * */
    fun getActionPendingIntent(download: Download, actionType: Int): PendingIntent?

    /** Gets an existing notification. If one does not exist.
     * This method calls createNotification and returns the newly created notification.
     * Called on a background notification thread.
     * @param download the download
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     *         -1 if the eta is unknown.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     *         -1 if the downloadedBytesPerSecond is unknown or not provided.
     * @return The notification associated with a Download or null if the notification manager is not managing
     * the downloads notification.
     * */
    fun getNotification(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Notification?

    /** Cancels an existing notification for a download.
     * Called on a background notification thread.
     * @param download the download
     * @return returns true if the cancel was handled by this notification manager otherwise false.
     * */
    fun cancelNotification(download: Download): Boolean

    /**
     * Updates the existing notification, or removed it when the notification is no longer needed.
     * Called on a background notification thread.
     * @param download the download
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     *         -1 if the eta is unknown.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     *         -1 if the downloadedBytesPerSecond is unknown or not provided.
     * @return returns true if the post update was handled by this notification manager otherwise false.
     * */
    fun postNotificationUpdate(download: Download, etaInMilliSeconds: Long = -1, downloadedBytesPerSecond: Long = -1): Boolean

    /** Called when a download completes. The status of the download will be Status.COMPLETED.
     * Called on a background notification thread.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @return returns true if this notification manager is handling this downloads notification otherwise false.
     * */
    fun onCompleted(download: Download): Boolean

    /** Called when an error occurs when downloading a download. The status of the download will be
     * Status.FAILED. See the download error field on the download for more information
     * on the specific error that occurred.
     * Called on a background notification thread.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @return returns true if this notification manager is handling this downloads notification otherwise false.
     * */
    fun onError(download: Download): Boolean

    /**
     * Called to report that the download process has started for a request. The status of the download
     * will be Status.DOWNLOADING.
     * Called on a background notification thread.
     * @param download An immutable object which contains a current snapshot of all the information
     * @param downloadBlocks list of download's downloading blocks information.
     * @param totalBlocks total downloading blocks for a download.
     * about a specific download managed by Fetch.
     * @return returns true if this notification manager is handling this downloads notification otherwise false.
     * */
    fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int): Boolean

    /** Called several times to report the progress of a download when downloading.
     * The status of the download will be Status.DOWNLOADING.
     * Called on a background notification thread.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     * Can return -1 to indicate that the estimated time remaining is unknown.
     * @return returns true if this notification manager is handling this downloads notification otherwise false.
     * */
    fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Boolean

    /** Called when a download is paused. The status of the download will be Status.PAUSED.
     * Called on a background notification thread.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @return returns true if this notification manager is handling this downloads notification otherwise false.
     * */
    fun onPaused(download: Download): Boolean

    /** Called when a download is un-paused and queued again for download.
     * The status of the download will be Status.QUEUED.
     * Called on a background notification thread.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @return returns true if this notification manager is handling this downloads notification otherwise false.
     * */
    fun onResumed(download: Download): Boolean

    /** Called when a download is cancelled. The status of the download will be
     * Status.CANCELLED. The file for this download is not deleted.
     * Called on a background notification thread.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @return returns true if this notification manager is handling this downloads notification otherwise false.
     * */
    fun onCancelled(download: Download): Boolean

    /** Called when a download is removed and is no longer managed by Fetch or
     * contained in the Fetch database. The file for this download is not deleted.
     * The status of a download will be Status.REMOVED.
     * Called on a background notification thread.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @return returns true if this notification manager is handling this downloads notification otherwise false.
     * */
    fun onRemoved(download: Download): Boolean

    /** Called when a download is deleted and is no longer managed by Fetch or contained in
     * the fetch database. The downloaded file is deleted. The status of a download will be
     * Status.DELETED.
     * Called on a background notification thread.
     * @param download An immutable object which contains a current snapshot of all the information
     * about a specific download managed by Fetch.
     * @return returns true if this notification manager is handling this downloads notification otherwise false.
     * */
    fun onDeleted(download: Download): Boolean

}