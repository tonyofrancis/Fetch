package com.tonyodev.fetch2notifications

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS

/**
 * The default notification manager class for Fetch. Extend this class to provide your own
 * custom implementation. The updateNotificationBuilder,  createONotificationChannels and getChannelId
 * methods are probably the only methods you will need to override.
 * */
open class DefaultFetchNotificationManager(
        /**Context*/
        protected val context: Context,
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

    protected val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    protected val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    protected val activeNotificationsBuilderMap = mutableMapOf<Int, NotificationCompat.Builder>()
    override var progressReportingIntervalInMillis: Long = DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS

    init {
        initialize()
    }

    private fun initialize() {
        createONotificationChannels()
    }

    override fun createONotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultChannelIdString = context.getString(defaultChannelId)
            var channel: NotificationChannel? = notificationManager.getNotificationChannel(defaultChannelIdString)
            if (channel == null) {
                val defaultChannelNameString = context.getString(defaultChannelName)
                channel = NotificationChannel(defaultChannelIdString, defaultChannelNameString, defaultChannelImportance)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun getChannelId(download: Download): String {
        return context.getString(defaultChannelId)
    }

    override fun updateNotificationBuilder(notificationBuilder: NotificationCompat.Builder,
                                           download: Download,
                                           etaInMilliSeconds: Long,
                                           downloadedBytesPerSecond: Long) {
        val progress = if (download.progress < 0) 0 else download.progress
        val maxProgress = if (download.total == -1L) 0 else 100
        val progressIndeterminate = download.total == -1L
        val title = getContentTitle(download)
        val contentText = getContentText(context, download, etaInMilliSeconds)
        val smallIcon = if (download.status == Status.DOWNLOADING) {
            notificationBuilder.setOngoing(true)
            android.R.drawable.stat_sys_download
        } else {
            notificationBuilder.setOngoing(false)
            android.R.drawable.stat_sys_download_done
        }
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(contentText)
                .setProgress(maxProgress, progress, progressIndeterminate)
        when (download.status) {
            Status.DOWNLOADING -> {
                notificationBuilder.addAction(R.drawable.fetch_notification_pause,
                        context.getString(R.string.fetch_notification_download_pause),
                        getActionPendingIntent(download, ACTION_TYPE_PAUSE))
                        .addAction(R.drawable.fetch_notification_cancel,
                                context.getString(R.string.fetch_notification_download_cancel),
                                getActionPendingIntent(download, ACTION_TYPE_CANCEL))
            }
            Status.PAUSED -> {
                notificationBuilder.addAction(R.drawable.fetch_notification_resume,
                        context.getString(R.string.fetch_notification_download_resume),
                        getActionPendingIntent(download, ACTION_TYPE_RESUME))
                        .addAction(R.drawable.fetch_notification_cancel,
                                context.getString(R.string.fetch_notification_download_cancel),
                                getActionPendingIntent(download, ACTION_TYPE_CANCEL))
            }
            else -> {

            }
        }
    }

    override fun pauseOnUnexpectedClose(download: Download): Boolean {
        return false
    }

    override fun getNotification(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Notification? {
        val notificationBuilder = activeNotificationsBuilderMap[download.id]
                ?: NotificationCompat.Builder(context, getChannelId(download))
        activeNotificationsBuilderMap[download.id] = notificationBuilder
        notificationBuilder.mActions.clear()
        updateNotificationBuilder(notificationBuilder, download, etaInMilliSeconds, downloadedBytesPerSecond)
        return notificationBuilder.build()
    }

    override fun postNotificationUpdate(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Boolean {
        return synchronized(activeNotificationsBuilderMap) {
            val notification = getNotification(download, etaInMilliSeconds, downloadedBytesPerSecond)
            if (notification != null) {
                val alarmIntent = Intent(ACTION_NOTIFICATION_CHECK)
                alarmIntent.putExtra(EXTRA_FETCH_NAMESPACE, download.namespace)
                alarmIntent.putExtra(EXTRA_DOWNLOAD_ID, download.id)
                alarmIntent.putExtra(EXTRA_DOWNLOAD_STATUS, download.status.value)
                if (pauseOnUnexpectedClose(download)) {
                    alarmIntent.putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_PAUSE)
                }
                val pendingIntent = PendingIntent.getBroadcast(context, download.id, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                alarmManager.cancel(pendingIntent)
                when (download.status) {
                    Status.DOWNLOADING,
                    Status.QUEUED -> {
                        val alarmTimeMillis = SystemClock.elapsedRealtime() + progressReportingIntervalInMillis + 3000
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTimeMillis, pendingIntent)
                        } else {
                            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTimeMillis, pendingIntent)
                        }
                    }
                    else -> {

                    }
                }
                notificationManager.notify(download.id, notification)
                true
            } else {
                false
            }
        }
    }

    override fun cancelNotification(download: Download): Boolean {
        return synchronized(activeNotificationsBuilderMap) {
            if (activeNotificationsBuilderMap.contains(download.id)) {
                notificationManager.cancel(download.id)
                activeNotificationsBuilderMap.remove(download.id)
                true
            } else {
                false
            }
        }
    }

    override fun getActionPendingIntent(download: Download, actionType: Int): PendingIntent? {
        val intent = Intent(ACTION_NOTIFICATION_ACTION)
        intent.putExtra(EXTRA_ACTION_TYPE, actionType)
        intent.putExtra(EXTRA_FETCH_NAMESPACE, download.namespace)
        intent.putExtra(EXTRA_DOWNLOAD_ID, download.id)
        return PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
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