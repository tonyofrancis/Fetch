package com.tonyodev.fetch2

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.support.v4.app.NotificationCompat

/**
 * Implement this interface to create, maintain and show notifications
 * for Fetch downloads.
 * */
interface FetchNotificationManager {

    /**
     * The progress reporting interval time used to report the downloads status changes by Fetch.
     * The Fetch instance associated with this FetchNotificationManager sets this value automatically.
     * */
    var progressReportingIntervalInMillis: Long

    /**
     * Created a new notification for a group of downloads. Called on a background notification thread.
     * @param notificationId the notification id
     * @param notificationBuilder the notification builder used to create/update the notification for a download.
     * @param downloadNotifications list of download notifications. If grouped this list will have more
     *        than on download notification object. Otherwise it will only contain one download notification object.
     * @param context context
     */
    fun updateGroupNotificationBuilder(notificationId: Int,
                                       notificationBuilder: NotificationCompat.Builder,
                                       downloadNotifications: List<DownloadNotification>,
                                       context: Context)


    /**
     * Created a new notification for a download. Called on a background notification thread.
     * @param notificationBuilder the notification builder used to create/update the notification for a download.
     * @param downloadNotification download notification object.
     * @param context context
     */
    fun updateNotificationBuilder(notificationBuilder: NotificationCompat.Builder,
                                  downloadNotification: DownloadNotification,
                                  context: Context)


    /**
     * Get an action pending intent for a download notification for the specified actionType.
     * @param downloadNotification the download notification.
     * @param actionType the actionType.
     * @return the pending intent.
     * */
    fun getActionPendingIntent(downloadNotification: DownloadNotification, actionType: DownloadNotification.ActionType): PendingIntent

    /**
     * Called by the associated Fetch instance to cancel all ongoing notifications(Status.Queued or Status.Downloading)
     * when the Fetch instance is being closed and no longer being used.
     * Called on a background thread.
     * */
    fun cancelOngoingNotifications()

    /** Cancels an existing notification for a download.
     * Called on a background notification thread.
     * @param notificationId the notification id
     * */
    fun cancelNotification(notificationId: Int)

    /**
     * Creates and sets the notification channels for Android O + devices.
     * @param context context
     * @param notificationManager notification manager
     * */
    fun createNotificationChannels(context: Context, notificationManager: NotificationManager)

    /** Get the notificationId associated with a download.
     * @param download the download
     * @param context context
     * @return Return a unique id for a single download notification, or a unique id for a group
     *         download notification. The unique id should always be the same for the download
     *         each time this method is called.
     * */
    fun getNotificationId(download: Download, context: Context): Int

    /** Get the channel associated with a notification Id
     * @param notificationId the notification id
     * @param context context
     * @return channel id
     * */
    fun getChannelId(notificationId: Int, context: Context): String

    /**
     * Updates the existing notification, or removed it when the notification is no longer needed.
     * Called on a background notification thread. This method is called by the associated fetch instance
     * to create/update notifications for a download.
     * @param download the download
     * @param etaInMilliSeconds Estimated time remaining in milliseconds for the download to complete.
     *         -1 if the eta is unknown.
     * @param downloadedBytesPerSecond Average downloaded bytes per second.
     *         -1 if the downloadedBytesPerSecond is unknown or not provided.
     * @return returns true if the post update was handled by this notification manager otherwise false.
     * */
    fun postNotificationUpdate(download: Download, etaInMilliSeconds: Long = -1, downloadedBytesPerSecond: Long = -1): Boolean

}