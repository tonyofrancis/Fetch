package com.tonyodev.fetch2

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat

/**
 * The default notification manager class for Fetch. Extend this class to provide your own
 * custom implementation. The updateNotificationBuilder,  createNotificationChannels and getChannelId
 * methods are probably the only methods you will need to override when extending this class.
 * */
open class DefaultFetchNotificationManager(
        /**Context*/
        context: Context,
        /** The default notification channel id used for new notifications.*/
        @StringRes protected val defaultChannelId: Int,
        /** Default Channel name*/
        @StringRes protected val defaultChannelName: Int,
        /** Default Channel importance.*/
        protected val defaultChannelImportance: Int) : FetchNotificationManager {

    /** Pre Android O constructor.*/
    @JvmOverloads
    constructor(
            /**Context*/
            context: Context,
            /** The default notification channel id used for new notifications.*/
            @StringRes defaultChannelId: Int = R.string.fetch_notification_default_channel_id)
            : this(context, defaultChannelId, R.string.fetch_notification_default_channel_name, 3)

    /**Context*/
    protected val appContext: Context = context.applicationContext
    protected val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    protected val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    protected val activeNotificationsBuilderMap = mutableMapOf<Int, NotificationCompat.Builder>()
    protected val ongoingNotificationsSet = mutableSetOf<Int>()
    override var progressReportingIntervalInMillis: Long = 0L

    init {
        initialize()
    }

    private fun initialize() {
        createNotificationChannels()
    }

    override fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultChannelIdString = appContext.getString(defaultChannelId)
            var channel: NotificationChannel? = notificationManager.getNotificationChannel(defaultChannelIdString)
            if (channel == null) {
                val defaultChannelNameString = appContext.getString(defaultChannelName)
                channel = NotificationChannel(defaultChannelIdString, defaultChannelNameString, defaultChannelImportance)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun getChannelId(download: Download): String {
        return appContext.getString(defaultChannelId)
    }

    override fun updateNotificationBuilder(notificationBuilder: NotificationCompat.Builder,
                                           download: Download,
                                           etaInMilliSeconds: Long,
                                           downloadedBytesPerSecond: Long) {
        val smallIcon = if (download.status == Status.DOWNLOADING) {
            notificationBuilder.setOngoing(true)
            android.R.drawable.stat_sys_download
        } else {
            notificationBuilder.setOngoing(false)
            android.R.drawable.stat_sys_download_done
        }
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(smallIcon)
                .setContentTitle(getContentTitle(download))
                .setContentText(getContentText(appContext, download, etaInMilliSeconds))
        if (download.status == Status.FAILED) {
            notificationBuilder.setProgress(0, 0, false)
        } else {
            notificationBuilder.setProgress(if (download.total == -1L) 0 else 100,
                    if (download.progress < 0) 0 else download.progress
                    , download.total == -1L)
        }
        when (download.status) {
            Status.DOWNLOADING -> {
                notificationBuilder.addAction(R.drawable.fetch_notification_pause,
                        appContext.getString(R.string.fetch_notification_download_pause),
                        getActionPendingIntent(download, ACTION_TYPE_PAUSE))
                        .addAction(R.drawable.fetch_notification_cancel,
                                appContext.getString(R.string.fetch_notification_download_cancel),
                                getActionPendingIntent(download, ACTION_TYPE_CANCEL))
            }
            Status.PAUSED -> {
                notificationBuilder.addAction(R.drawable.fetch_notification_resume,
                        appContext.getString(R.string.fetch_notification_download_resume),
                        getActionPendingIntent(download, ACTION_TYPE_RESUME))
                        .addAction(R.drawable.fetch_notification_cancel,
                                appContext.getString(R.string.fetch_notification_download_cancel),
                                getActionPendingIntent(download, ACTION_TYPE_CANCEL))
            }
            else -> {

            }
        }
    }

    override fun getNotification(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Notification? {
        val notificationBuilder = activeNotificationsBuilderMap[download.id]
                ?: NotificationCompat.Builder(appContext, getChannelId(download))
        activeNotificationsBuilderMap[download.id] = notificationBuilder
        notificationBuilder.mActions.clear()
        updateNotificationBuilder(notificationBuilder, download, etaInMilliSeconds, downloadedBytesPerSecond)
        when (download.status) {
            Status.QUEUED,
            Status.DOWNLOADING -> {
                ongoingNotificationsSet.add(download.id)
            }
            else -> {
                ongoingNotificationsSet.remove(download.id)
            }
        }
        return notificationBuilder.build()
    }

    override fun postNotificationUpdate(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Boolean {
        return synchronized(activeNotificationsBuilderMap) {
            when (download.status) {
                Status.DELETED,
                Status.CANCELLED,
                Status.REMOVED -> {
                    cancelNotification(download)
                }
                else -> {
                    val notification = getNotification(download, etaInMilliSeconds, downloadedBytesPerSecond)
                    if (notification != null) {
                        if (progressReportingIntervalInMillis > 0) {
                            val alarmPendingIntent = getAlarmPendingIntent(download)
                            alarmManager.cancel(alarmPendingIntent)
                            when (download.status) {
                                Status.DOWNLOADING,
                                Status.QUEUED -> {
                                    val alarmTimeMillis = SystemClock.elapsedRealtime() + progressReportingIntervalInMillis
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTimeMillis, alarmPendingIntent)
                                    } else {
                                        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTimeMillis, alarmPendingIntent)
                                    }
                                }
                                else -> {

                                }
                            }
                        }
                        notificationManager.notify(download.id, notification)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    override fun cancelNotification(download: Download): Boolean {
        return synchronized(activeNotificationsBuilderMap) {
            if (activeNotificationsBuilderMap.contains(download.id)) {
                notificationManager.cancel(download.id)
                activeNotificationsBuilderMap.remove(download.id)
                ongoingNotificationsSet.remove(download.id)
                true
            } else {
                false
            }
        }
    }

    override fun cancelOngoingNotifications() {
        synchronized(activeNotificationsBuilderMap) {
            for (notificationId in ongoingNotificationsSet) {
                notificationManager.cancel(notificationId)
                activeNotificationsBuilderMap.remove(notificationId)
            }
            ongoingNotificationsSet.clear()
        }
    }

    override fun getActionPendingIntent(download: Download, actionType: Int): PendingIntent? {
        val intent = Intent(ACTION_NOTIFICATION_ACTION)
        intent.putExtra(EXTRA_ACTION_TYPE, actionType)
        intent.putExtra(EXTRA_NAMESPACE, download.namespace)
        intent.putExtra(EXTRA_DOWNLOAD_ID, download.id)
        return PendingIntent.getBroadcast(appContext, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getAlarmPendingIntent(download: Download): PendingIntent {
        val alarmIntent = Intent(ACTION_NOTIFICATION_CHECK)
        alarmIntent.putExtra(EXTRA_NAMESPACE, download.namespace)
        alarmIntent.putExtra(EXTRA_DOWNLOAD_ID, download.id)
        alarmIntent.putExtra(EXTRA_DOWNLOAD_STATUS, download.status.value)
        return PendingIntent.getBroadcast(appContext, download.id, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun getContentTitle(download: Download): String {
        return download.fileUri.lastPathSegment ?: Uri.parse(download.url).lastPathSegment
        ?: download.url
    }

    private fun getContentText(context: Context, download: Download, etaInMilliSeconds: Long): String {
        return when {
            download.status == Status.COMPLETED -> context.getString(R.string.fetch_notification_download_complete)
            download.status == Status.FAILED -> context.getString(R.string.fetch_notification_download_failed)
            download.status == Status.PAUSED -> context.getString(R.string.fetch_notification_download_paused)
            download.status == Status.QUEUED -> context.getString(R.string.fetch_notification_download_starting)
            etaInMilliSeconds < 0 -> context.getString(R.string.fetch_notification_download_downloading)
            else -> {
                var seconds = (etaInMilliSeconds / 1000)
                val hours = (seconds / 3600)
                seconds -= (hours * 3600)
                val minutes = (seconds / 60)
                seconds -= (minutes * 60)
                when {
                    hours > 0 -> context.getString(R.string.fetch_notification_download_eta_hrs, hours, minutes, seconds)
                    minutes > 0 -> context.getString(R.string.fetch_notification_download_eta_min, minutes, seconds)
                    else -> context.getString(R.string.fetch_notification_download_eta_sec, seconds)
                }
            }
        }
    }

}