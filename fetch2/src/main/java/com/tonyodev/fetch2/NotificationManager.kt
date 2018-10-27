package com.tonyodev.fetch2

import android.app.Notification
import android.app.PendingIntent

interface NotificationManager : FetchListener {

    /**
     * Created a new notification for the download.
     * @param download the download
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     *         -1 if the eta is unknown.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     *         -1 if the downloadedBytesPerSecond is unknown or not provided.
     * @return The notification associated with a download
     * */
    fun buildNotification(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Notification


    fun getActionPendingIntent(download: Download, actionType: Int): PendingIntent

    /** Gets an existing notification. If one does not exist
     * this method calls createNotification and returns the newly created notification.
     * @param download the download
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     *         -1 if the eta is unknown.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     *         -1 if the downloadedBytesPerSecond is unknown or not provided.
     * @return The notification associated with a Download.
     * */
    fun getNotification(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Notification

    /** Cancels an existing notification for a download.
     * @param download the download
     * */
    fun cancelNotification(download: Download)

    /**
     * Updates the existing notification, or removed it when the notification is no longer needed.
     * @param download the download
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     *         -1 if the eta is unknown.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     *         -1 if the downloadedBytesPerSecond is unknown or not provided.
     * */
    fun postNotificationUpdate(download: Download, etaInMilliSeconds: Long = -1, downloadedBytesPerSecond: Long = -1)

}