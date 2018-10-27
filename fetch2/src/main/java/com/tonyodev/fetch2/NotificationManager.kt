package com.tonyodev.fetch2

import android.app.Notification
import android.app.PendingIntent
import android.support.v4.app.NotificationCompat

/**
 * Implement this interface to create, maintain and show notifications
 * for downloads.
 * */
interface NotificationManager : FetchListener {

    /**
     * Created a new notification for the download. Called on a background notification thread.
     * @param download the download
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     *         -1 if the eta is unknown.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     *         -1 if the downloadedBytesPerSecond is unknown or not provided.
     * @return The notification associated with a download
     * */
    fun updateNotificationBuilder(notificationBuilder: NotificationCompat.Builder, download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long)

    /**
     * Get a new PendingIntent associated with a notification action type.
     * Called on a background notification thread.
     * @param download the download
     * @param actionType the action the intent will perform when the user clicks the action
     *        on the notification. See the FetchIntent.kt file for the different action types.
     * @return the action pending intent
     *
     * */
    fun getActionPendingIntent(download: Download, actionType: Int): PendingIntent

    /** Gets an existing notification. If one does not exist.
     * This method calls createNotification and returns the newly created notification.
     * Called on a background notification thread.
     * @param download the download
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     *         -1 if the eta is unknown.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     *         -1 if the downloadedBytesPerSecond is unknown or not provided.
     * @return The notification associated with a Download.
     * */
    fun getNotification(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Notification

    /** Cancels an existing notification for a download.
     * Called on a background notification thread.
     * @param download the download
     * */
    fun cancelNotification(download: Download)

    /**
     * Updates the existing notification, or removed it when the notification is no longer needed.
     * Called on a background notification thread.
     * @param download the download
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     *         -1 if the eta is unknown.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     *         -1 if the downloadedBytesPerSecond is unknown or not provided.
     * */
    fun postNotificationUpdate(download: Download, etaInMilliSeconds: Long = -1, downloadedBytesPerSecond: Long = -1)

}