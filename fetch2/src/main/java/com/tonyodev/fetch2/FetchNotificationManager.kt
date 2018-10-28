package com.tonyodev.fetch2

import android.app.Notification
import android.app.PendingIntent
import android.support.v4.app.NotificationCompat

/**
 * Implement this interface to create, maintain and show notifications
 * for Fetch downloads.
 * */
interface FetchNotificationManager {

    /**
     * The progress reporting interval time used to report the downloads status changes.
     * The Fetch instance associated with this FetchNotificationManager sets this value automatically.
     * */
    var progressReportingIntervalInMillis: Long

    /**
     * Creates and sets the notification channels for Android O devices.
     * */
    fun createONotificationChannels()

    /**
     * Returns the channel id for a download notification.
     * @param download the download
     * @return channel id
     * */
    fun getChannelId(download: Download): String

    /**
     * If a download has a status of Queue or Downloading and the application
     * was closed or the Fetch instance managing the download was released unexpectedly,
     * the notification manager can send a signal to pause the download. The default is false.
     * @param download the download
     * @return true to pause the download otherwise false. The default is false.
     * */
    fun pauseOnUnexpectedClose(download: Download): Boolean

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

}