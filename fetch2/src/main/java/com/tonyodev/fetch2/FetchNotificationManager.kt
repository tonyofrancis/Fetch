package com.tonyodev.fetch2

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.support.v4.app.NotificationCompat

/**
 * Implement this interface to create, maintain and show notifications
 * for Fetch downloads.
 * */
interface FetchNotificationManager {

    /** The notification manager intent filter action.*/
    val notificationManagerAction: String

    /**
     * The notification manager's broadcast receiver
     * that intercepts notification actions of notifications
     * it posted.
     * */
    val broadcastReceiver: BroadcastReceiver

    /**
     * Create the notification for a group of downloads. Called on a background notification thread.
     * @param groupId the group unique id.
     * @param notificationBuilder the group summary notification builder.
     * @param downloadNotifications list of download notifications in the group.
     * @param context context
     * @return return true if download notifications should be grouped otherwise false.
     */
    fun updateGroupSummaryNotification(groupId: Int,
                                       notificationBuilder: NotificationCompat.Builder,
                                       downloadNotifications: List<DownloadNotification>,
                                       context: Context): Boolean

    /**
     * Create notification for a download. Called on a background notification thread.
     * @param notificationBuilder the notification builder
     * @param downloadNotification the download notification
     * @param context the context
     * */
    fun updateNotification(notificationBuilder: NotificationCompat.Builder,
                           downloadNotification: DownloadNotification,
                           context: Context)

    /**
     * Notify the notification manager that a group of download notifications have
     * been updated and should be recreated. Called on a background notification thread.
     * @param groupId the group id
     * */
    fun notify(groupId: Int)

    /**
     * Get a group action pending intent for a download notification for the specified actionType.
     * @param groupId the group id
     * @param downloadNotifications list of download notifications in the group.
     * @param actionType the group actionType. See DownloadNotification.ActionType.*_ALL enum.
     * @return the pending intent.
     * */
    fun getGroupActionPendingIntent(groupId: Int,
                                    downloadNotifications: List<DownloadNotification>,
                                    actionType: DownloadNotification.ActionType): PendingIntent

    /**
     * Get an action pending intent for a download notification for the specified actionType.
     * @param downloadNotification the download notification.
     * @param actionType the actionType. See DownloadNotification.ActionType enum.
     * @return the pending intent.
     * */
    fun getActionPendingIntent(downloadNotification: DownloadNotification, actionType: DownloadNotification.ActionType): PendingIntent

    /**
     * Called by the associated Fetch instance to cancel all ongoing notifications(Status.Queued or Status.Downloading)
     * when the Fetch instance is being closed and no longer being used.
     * Called on a notification background thread.
     * */
    fun cancelOngoingNotifications()

    /** Cancels an existing notification for a download.
     * Called on a background notification thread.
     * @param notificationId the notification id
     * */
    fun cancelNotification(notificationId: Int)

    /**
     * Creates and sets the notification channels for Android O + devices. Override this method
     * to create your own notifications channels.
     * @param context context
     * @param notificationManager notification manager
     * */
    fun createNotificationChannels(context: Context, notificationManager: NotificationManager)

    /** Get the channel associated with a notification Id
     * @param notificationId the notification id. This can be a single download notification id or the group notificationId
     * @param context context
     * @return channel id
     * */
    fun getChannelId(notificationId: Int, context: Context): String

    /**
     * Add/Updates the existing download notification, or removed it when the notification is no longer needed.
     * Called on a background notification thread. This method is called by the associated fetch instance
     * to create/update notifications for a download.
     * @param download the download
     * @return returns true if the post update was handled by this notification manager otherwise false.
     * */
    fun postDownloadUpdate(download: Download): Boolean

    /**
     * Gets the notification builder for a download notification.
     * @param notificationId the download notification id.
     * @param groupId the download group id.
     * @return the notification builder.
     * */
    fun getNotificationBuilder(notificationId: Int, groupId: Int): NotificationCompat.Builder

    /**
     * Indicates if the download notification should update an existing notification.
     * @param downloadNotification the download notification.
     * @return true if the existing notification should be updated. False otherwise.
     * */
    fun shouldUpdateNotification(downloadNotification: DownloadNotification): Boolean

    /**
     * Indicates if the download notification should be cancelled.
     * @param downloadNotification the download notification.
     * @return true if the existing notification should be cancelled. False otherwise.
     * */
    fun shouldCancelNotification(downloadNotification: DownloadNotification): Boolean

    /**
     * Registers the notification manager broadcast receiver
     * */
    fun registerBroadcastReceiver()

    /**
     * UnRegisters the notification manager broadcast receiver
     * */
    fun unregisterBroadcastReceiver()

    /**
     * Gets the Fetch Instance for the namespace.
     * @param namespace the fetch namespace.
     * @return fetch instance.
     * */
    fun getFetchInstanceForNamespace(namespace: String): Fetch

    /**
     * Returns the time in millis before a notification
     * is cancelled because it was not updated or have an active status.
     *
     * Note: This value should be updated if you have set a different
     * value for progress interval on a FetchConfiguration. Also
     * this value should be more than the progress interval update.
     * */
    fun getNotificationTimeOutMillis(): Long

    /**
     * Gets the title for a download notification.
     * @param download the download
     * @return the title for the download which will be displayed in the
     * notification.
     * */
    fun getDownloadNotificationTitle(download: Download): String

    /**
     * Gets the text that will be displayed on the right side of the notification.
     * Eg: eta, download status, etc.
     * @param context the context
     * @param downloadNotification the download notification
     * @return the subtitle text
     * */
    fun getSubtitleText(context: Context, downloadNotification: DownloadNotification): String

}