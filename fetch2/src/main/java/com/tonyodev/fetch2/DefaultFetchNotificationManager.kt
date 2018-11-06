package com.tonyodev.fetch2

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.support.v4.app.NotificationCompat

import com.tonyodev.fetch2.DownloadNotification.ActionType.*

/**
 * The default notification manager class for Fetch. This manager supports both single
 * download notifications and grouped download notifications. Extend this class to provide your own
 * custom implementation.
 * */
open class DefaultFetchNotificationManager(context: Context) : FetchNotificationManager {

    private val context: Context = context.applicationContext
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val downloadNotificationsMap = mutableMapOf<Int, DownloadNotification>()
    private val downloadNotificationsBuilderMap = mutableMapOf<Int, NotificationCompat.Builder>()
    override var progressReportingIntervalInMillis: Long = 0L

    init {
        initialize()
    }

    private fun initialize() {
        createNotificationChannels(context, notificationManager)
    }

    override fun createNotificationChannels(context: Context, notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = context.getString(R.string.fetch_notification_default_channel_id)
            var channel: NotificationChannel? = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                val channelName = context.getString(R.string.fetch_notification_default_channel_name)
                channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun updateNotifications(groupId: Int,
                                     groupSummaryNotificationBuilder: NotificationCompat.Builder,
                                     downloadNotifications: List<DownloadNotification>,
                                     context: Context) {
        val smallIcon = if (downloadNotifications.any { it.isDownloading }) {
            android.R.drawable.stat_sys_download
        } else {
            android.R.drawable.stat_sys_download_done
        }
        val style = NotificationCompat.InboxStyle()
        for (downloadNotification in downloadNotifications) {
            val title = getContentTitle(downloadNotification)
            val contentTitle = getContentText(context, downloadNotification)
            style.addLine("$title $contentTitle")
        }
        groupSummaryNotificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(smallIcon)
                .setContentTitle(context.getString(R.string.fetch_notification_default_channel_name))
                .setContentText("")
                .setStyle(style)
                .setOngoing(downloadNotifications.any { it.isOnGoingNotification })
                .setGroup(groupId.toString())
                .setGroupSummary(true)
        for (downloadNotification in downloadNotifications) {
            val notificationId = downloadNotification.notificationId
            val notificationBuilder = getNotificationBuilder(notificationId)
            updateDownloadNotification(notificationBuilder, downloadNotification, context)
            handleNotificationAlarm(notificationId, downloadNotification.isOnGoingNotification, arrayOf())
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    /**
     * Updates a single download notification that includes download actions.
     * @param notificationBuilder the download notification builder.
     * @param downloadNotification the download notification.
     * @param context context
     * */
    open fun updateDownloadNotification(notificationBuilder: NotificationCompat.Builder,
                                        downloadNotification: DownloadNotification,
                                        context: Context) {
        val smallIcon = if (downloadNotification.isDownloading) {
            android.R.drawable.stat_sys_download
        } else {
            android.R.drawable.stat_sys_download_done
        }
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(smallIcon)
                .setContentTitle(getContentTitle(downloadNotification))
                .setContentText(getContentText(context, downloadNotification))
                .setOngoing(downloadNotification.isOnGoingNotification)
                .setGroup(downloadNotification.groupId.toString())
                .setGroupSummary(false)
        if (downloadNotification.isFailed) {
            notificationBuilder.setProgress(0, 0, false)
        } else {
            val progressIndeterminate = downloadNotification.progressIndeterminate
            val maxProgress = if (downloadNotification.progressIndeterminate) 0 else 100
            val download = downloadNotification.download
            val progress = if (download.progress < 0) 0 else download.progress
            notificationBuilder.setProgress(maxProgress, progress, progressIndeterminate)
        }
        when {
            downloadNotification.isDownloading -> {
                notificationBuilder.addAction(R.drawable.fetch_notification_pause,
                        context.getString(R.string.fetch_notification_download_pause),
                        getActionPendingIntent(downloadNotification, PAUSE))
                        .addAction(R.drawable.fetch_notification_cancel,
                                context.getString(R.string.fetch_notification_download_cancel),
                                getActionPendingIntent(downloadNotification, CANCEL))
            }
            downloadNotification.isPaused -> {
                notificationBuilder.addAction(R.drawable.fetch_notification_resume,
                        context.getString(R.string.fetch_notification_download_resume),
                        getActionPendingIntent(downloadNotification, RESUME))
                        .addAction(R.drawable.fetch_notification_cancel,
                                context.getString(R.string.fetch_notification_download_cancel),
                                getActionPendingIntent(downloadNotification, CANCEL))
            }
        }
        val contentPendingIntent = downloadNotification.contentPendingIntent
        if (contentPendingIntent != null) {
            notificationBuilder.setContentIntent(contentPendingIntent)
        }
    }

    override fun getActionPendingIntent(downloadNotification: DownloadNotification,
                                        actionType: DownloadNotification.ActionType): PendingIntent {
        synchronized(downloadNotificationsMap) {
            val intent = Intent(ACTION_NOTIFICATION_ACTION)
            intent.putExtra(EXTRA_NAMESPACE, downloadNotification.download.namespace)
            intent.putExtra(EXTRA_DOWNLOAD_ID, downloadNotification.download.id)
            intent.putExtra(EXTRA_NOTIFICATION_ID, downloadNotification.notificationId)
            val action = when (actionType) {
                CANCEL -> ACTION_TYPE_CANCEL
                DELETE -> ACTION_TYPE_DELETE
                RESUME -> ACTION_TYPE_RESUME
                PAUSE -> ACTION_TYPE_PAUSE
            }
            intent.putExtra(EXTRA_ACTION_TYPE, action)
            return PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_CANCEL_CURRENT)
        }
    }

    override fun cancelOngoingNotifications() {
        synchronized(downloadNotificationsMap) {
            val iterator = downloadNotificationsMap.values.iterator()
            while (iterator.hasNext()) {
                val downloadNotification = iterator.next()
                if (downloadNotification.isActive) {
                    notificationManager.cancel(downloadNotification.notificationId)
                    downloadNotificationsBuilderMap.remove(downloadNotification.notificationId)
                    notify(downloadNotification.groupId)
                    iterator.remove()
                }
            }
        }
    }

    override fun cancelNotification(notificationId: Int) {
        synchronized(downloadNotificationsMap) {
            notificationManager.cancel(notificationId)
            downloadNotificationsBuilderMap.remove(notificationId)
            val downloadNotification = downloadNotificationsMap[notificationId]
            if (downloadNotification != null) {
                downloadNotificationsMap.remove(notificationId)
                notify(downloadNotification.groupId)
            }
        }
    }

    override fun getChannelId(notificationId: Int, context: Context): String {
        return context.getString(R.string.fetch_notification_default_channel_id)
    }

    override fun postNotificationUpdate(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Boolean {
        return synchronized(downloadNotificationsMap) {
            val downloadNotification = downloadNotificationsMap[download.id]
                    ?: DownloadNotification(download)
            downloadNotification.notificationId = download.id
            downloadNotification.groupId = download.group
            downloadNotification.download = download
            downloadNotification.downloadedBytesPerSecond = downloadedBytesPerSecond
            downloadNotification.etaInMilliSeconds = etaInMilliSeconds
            downloadNotificationsMap[download.id] = downloadNotification
            if (downloadNotification.isCancelledNotification) {
                cancelNotification(downloadNotification.notificationId)
            } else {
                notify(download.group)
            }
            true
        }
    }

    /** Calls the updateNotifications method for the specified group. Call this method
     * anytime a download notification is added or removed from a group.
     * @param groupId the group id
     * */
    open fun notify(groupId: Int) {
        synchronized(downloadNotificationsMap) {
            val groupedDownloadNotifications = downloadNotificationsMap.values.filter { it.groupId == groupId }
            val ongoingNotification = groupedDownloadNotifications.any { it.isOnGoingNotification }
            val groupSummaryNotificationBuilder = getNotificationBuilder(groupId)
            updateNotifications(groupId, groupSummaryNotificationBuilder, groupedDownloadNotifications, context)
            handleNotificationAlarm(groupId, ongoingNotification, groupedDownloadNotifications.toTypedArray())
            notificationManager.notify(groupId, groupSummaryNotificationBuilder.build())
        }
    }

    /**
     * Handles canceling ongoing download notifications when fetch closes or the app closes.
     * @param notificationId the download notification id.
     * @param ongoingNotification true if the download notification is ongoing.
     * @param downloadNotifications array of download notifications in the group.
     * */
    open fun handleNotificationAlarm(notificationId: Int,
                                     ongoingNotification: Boolean,
                                     downloadNotifications: Array<DownloadNotification>) {
        synchronized(downloadNotificationsMap) {
            if (progressReportingIntervalInMillis > 0) {
                val alarmIntent = Intent(ACTION_NOTIFICATION_CHECK)
                alarmIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                alarmIntent.putExtra(EXTRA_DOWNLOAD_NOTIFICATIONS, downloadNotifications)
                val pendingIntent = PendingIntent.getBroadcast(context, notificationId, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                alarmManager.cancel(pendingIntent)
                if (ongoingNotification) {
                    val alarmTimeMillis = SystemClock.elapsedRealtime() + progressReportingIntervalInMillis
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTimeMillis, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTimeMillis, pendingIntent)
                    }
                }
            }
        }
    }

    /**
     * Gets the cached notification builder for a download notification.
     * @param notificationId the download notification id.
     * @return the cached notification builder.
     * */
    open fun getNotificationBuilder(notificationId: Int): NotificationCompat.Builder {
        synchronized(downloadNotificationsMap) {
            val notificationBuilder = downloadNotificationsBuilderMap[notificationId]
                    ?: NotificationCompat.Builder(context, getChannelId(notificationId, context))
            downloadNotificationsBuilderMap[notificationId] = notificationBuilder
            notificationBuilder
                    .setGroup(notificationId.toString())
                    .setStyle(null)
                    .setProgress(0, 0, false)
                    .setContentTitle(null)
                    .setContentText(null)
                    .setContentIntent(null)
                    .setGroupSummary(false)
                    .setOngoing(false)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .mActions.clear()
            return notificationBuilder
        }
    }

    private fun getContentTitle(downloadNotification: DownloadNotification): String {
        val download = downloadNotification.download
        return download.fileUri.lastPathSegment ?: Uri.parse(download.url).lastPathSegment
        ?: download.url
    }

    private fun getContentText(context: Context, downloadNotification: DownloadNotification): String {
        return when {
            downloadNotification.isCompleted -> context.getString(R.string.fetch_notification_download_complete)
            downloadNotification.isFailed -> context.getString(R.string.fetch_notification_download_failed)
            downloadNotification.isPaused -> context.getString(R.string.fetch_notification_download_paused)
            downloadNotification.isQueued -> context.getString(R.string.fetch_notification_download_starting)
            downloadNotification.etaInMilliSeconds < 0 -> context.getString(R.string.fetch_notification_download_downloading)
            else -> getEtaText(context, downloadNotification.etaInMilliSeconds)
        }
    }

    private fun getEtaText(context: Context, etaInMilliSeconds: Long): String {
        var seconds = (etaInMilliSeconds / 1000)
        val hours = (seconds / 3600)
        seconds -= (hours * 3600)
        val minutes = (seconds / 60)
        seconds -= (minutes * 60)
        return when {
            hours > 0 -> context.getString(R.string.fetch_notification_download_eta_hrs, hours, minutes, seconds)
            minutes > 0 -> context.getString(R.string.fetch_notification_download_eta_min, minutes, seconds)
            else -> context.getString(R.string.fetch_notification_download_eta_sec, seconds)
        }
    }

}