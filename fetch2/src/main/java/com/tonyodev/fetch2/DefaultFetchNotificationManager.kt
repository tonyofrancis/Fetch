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

    override fun updateGroupNotificationBuilder(notificationId: Int,
                                                notificationBuilder: NotificationCompat.Builder,
                                                downloadNotifications: List<DownloadNotification>,
                                                context: Context) {

        val smallIcon = if (downloadNotifications.any { it.isDownloading }) {
            android.R.drawable.stat_sys_download
        } else {
            android.R.drawable.stat_sys_download_done
        }
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(smallIcon)
                .setContentTitle("group title")
                .setContentText("group summary")
                .setStyle(NotificationCompat.InboxStyle()
                        .addLine("tonyo first line")
                        .addLine("tonyo second line"))
                .setGroup(notificationId.toString())
                .setGroupSummary(true)
    }

    override fun updateNotificationBuilder(notificationBuilder: NotificationCompat.Builder,
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
    }


    override fun getActionPendingIntent(downloadNotification: DownloadNotification,
                                        actionType: DownloadNotification.ActionType): PendingIntent {
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

    override fun cancelOngoingNotifications() {
        synchronized(downloadNotificationsMap) {
            val notificationIds = downloadNotificationsBuilderMap.keys
            for (notificationId in notificationIds) {
                notificationManager.cancel(notificationId)
            }
            downloadNotificationsBuilderMap.clear()
            downloadNotificationsMap.clear()
        }
    }

    override fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        downloadNotificationsBuilderMap.remove(notificationId)
    }

    override fun getNotificationId(download: Download, context: Context): Int {
        return download.group
    }

    override fun getChannelId(notificationId: Int, context: Context): String {
        return context.getString(R.string.fetch_notification_default_channel_id)
    }

    override fun postNotificationUpdate(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Boolean {
        return synchronized(downloadNotificationsMap) {
            val downloadNotification = downloadNotificationsMap[download.id]
                    ?: DownloadNotification(download)
            downloadNotification.notificationId = getNotificationId(download, context)
            downloadNotification.download = download
            downloadNotification.downloadedBytesPerSecond = downloadedBytesPerSecond
            downloadNotification.etaInMilliSeconds = etaInMilliSeconds
            downloadNotificationsMap[download.id] = downloadNotification
            val notification = getNotification(downloadNotification)
            handleNotificationAlarm(downloadNotification)
            if (notification != null) {
                notificationManager.notify(downloadNotification.notificationId, notification)
                true
            } else {
                false
            }
        }
    }

    private fun handleNotificationAlarm(downloadNotification: DownloadNotification) {
        if (progressReportingIntervalInMillis > 0) {
            val alarmIntent = Intent(ACTION_NOTIFICATION_CHECK)
            alarmIntent.putExtra(EXTRA_NOTIFICATION_ID, downloadNotification.notificationId)
            val pendingIntent = PendingIntent.getBroadcast(context, downloadNotification.notificationId, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT)
            alarmManager.cancel(pendingIntent)
            if (downloadNotification.isOnGoingNotification) {
                val alarmTimeMillis = SystemClock.elapsedRealtime() + progressReportingIntervalInMillis
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTimeMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTimeMillis, pendingIntent)
                }
            }
        }
    }

    private fun getNotification(downloadNotification: DownloadNotification): Notification? {
        return if (downloadNotification.isCancelledNotification) {
            downloadNotificationsMap.remove(downloadNotification.download.id)
            if (isGroupNotification(downloadNotification.notificationId)) {
                buildNotification(downloadNotification.notificationId)
            } else {
                cancelNotification(downloadNotification.notificationId)
                null
            }
        } else {
            buildNotification(downloadNotification.notificationId)
        }
    }

    private fun buildNotification(notificationId: Int): Notification {
        val notificationBuilder = getNotificationBuilder(notificationId)
        val downloadNotifications = downloadNotificationsMap.values.filter { it.notificationId == notificationId }
        if (downloadNotifications.isNotEmpty()) {
            if (downloadNotifications.size > 1) {
                updateGroupNotificationBuilder(notificationId, notificationBuilder, downloadNotifications, context)
            } else {
                updateNotificationBuilder(notificationBuilder, downloadNotifications.first(), context)
            }
        }
        return notificationBuilder.build()
    }

    private fun getNotificationBuilder(notificationId: Int): NotificationCompat.Builder {
        val notificationBuilder = downloadNotificationsBuilderMap[notificationId]
                ?: NotificationCompat.Builder(context, getChannelId(notificationId, context))
        downloadNotificationsBuilderMap[notificationId] = notificationBuilder
        val notificationGroupDownloads = downloadNotificationsMap.values.filter { it.notificationId == notificationId }
        val isOnGoingNotification = notificationGroupDownloads.any { it.isOnGoingNotification }
        notificationBuilder.setOngoing(isOnGoingNotification)
        var finished = false
        for (downloadNotification in notificationGroupDownloads) {
            if (downloadNotification.isRemovableNotification) {
                finished = true
            } else {
                finished = false
                break
            }
        }
        if (finished) {
            for (downloadNotification in notificationGroupDownloads) {
                downloadNotificationsMap.remove(downloadNotification.download.id)
            }
            downloadNotificationsBuilderMap.remove(notificationId)
        }
        if (notificationGroupDownloads.size > 1) {
            notificationBuilder.setGroupSummary(true)
        } else {
            notificationBuilder.setGroupSummary(false)
        }
        notificationBuilder
                .setGroup(notificationId.toString())
                .setStyle(null)
                .setProgress(0, 0, false)
                .mActions.clear()
        return notificationBuilder
    }

    private fun isGroupNotification(notificationId: Int): Boolean {
        return downloadNotificationsMap.values.count { it.notificationId == notificationId } > 1
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