package com.tonyodev.fetch2notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock

/**
 * The default notification manager class. Extend this class to provide your own
 * custom implementations. The build buildNotification method is probably the only method you
 * need to override.
 * */
open class DefaultNotificationManager(
        /**Context*/
        protected val context: Context,
        /** The default notification channel id used for new notifications.*/
        protected val notificationChannelId: String) : NotificationManager {

    protected val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    /**Map that holds notification builder for an active download.*/
    protected val activeNotificationsBuilderMap = mutableMapOf<Int, NotificationCompat.Builder>()

    override fun updateNotificationBuilder(notificationBuilder: NotificationCompat.Builder,
                                           download: Download,
                                           etaInMilliSeconds: Long,
                                           downloadedBytesPerSecond: Long) {
        val progress = if (download.progress < 0) 0 else download.progress
        val progressIndeterminate = download.total == -1L
        val title = getContentTitle(download)
        val contentText = getContentText(context, download, etaInMilliSeconds)
        val smallIcon = if (download.status == Status.DOWNLOADING) {
            android.R.drawable.stat_sys_download
        } else {
            android.R.drawable.stat_sys_download_done
        }
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(contentText)
                .setProgress(100, progress, progressIndeterminate)
        when (download.status) {
            Status.DOWNLOADING -> {
                notificationBuilder.setOngoing(true)
                        .addAction(R.drawable.fetch_notification_pause,
                                context.getString(R.string.fetch_notification_download_pause),
                                getActionPendingIntent(download, ACTION_TYPE_PAUSE))
                        .addAction(R.drawable.fetch_notification_cancel,
                                context.getString(R.string.fetch_notification_download_cancel),
                                getActionPendingIntent(download, ACTION_TYPE_CANCEL))
            }
            Status.PAUSED -> {
                notificationBuilder.setOngoing(true)
                        .addAction(R.drawable.fetch_notification_resume,
                                context.getString(R.string.fetch_notification_download_resume),
                                getActionPendingIntent(download, ACTION_TYPE_RESUME))
                        .addAction(R.drawable.fetch_notification_cancel,
                                context.getString(R.string.fetch_notification_download_cancel),
                                getActionPendingIntent(download, ACTION_TYPE_CANCEL))
            }
            else -> {
                notificationBuilder.setOngoing(false)
            }
        }
    }

    override fun getNotification(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Notification {
        val notificationBuilder = activeNotificationsBuilderMap[download.id]
                ?: NotificationCompat.Builder(context, notificationChannelId)
        activeNotificationsBuilderMap[download.id] = notificationBuilder
        notificationBuilder.mActions.clear()
        updateNotificationBuilder(notificationBuilder, download, etaInMilliSeconds, downloadedBytesPerSecond)
        return notificationBuilder.build()
    }

    override fun postNotificationUpdate(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
        notificationManager.notify(download.id, getNotification(download, etaInMilliSeconds, downloadedBytesPerSecond))
    }

    override fun cancelNotification(download: Download) {
        if (activeNotificationsBuilderMap.contains(download.id)) {
            notificationManager.cancel(download.id)
        }
        activeNotificationsBuilderMap.remove(download.id)
    }

    override fun getActionPendingIntent(download: Download, actionType: Int): PendingIntent {
        val intent = Intent(ACTION_NOTIFICATION_ACTION)
        intent.putExtra(EXTRA_ACTION_TYPE, actionType)
        intent.putExtra(EXTRA_FETCH_NAMESPACE, download.namespace)
        intent.putExtra(EXTRA_DOWNLOAD_ID, download.id)
        return PendingIntent.getBroadcast(context, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCompleted(download: Download) {
        postNotificationUpdate(download)
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        postNotificationUpdate(download)
    }

    override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
        postNotificationUpdate(download)
    }

    override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
        postNotificationUpdate(download, etaInMilliSeconds, downloadedBytesPerSecond)
    }

    override fun onPaused(download: Download) {
        postNotificationUpdate(download)
    }

    override fun onResumed(download: Download) {
        postNotificationUpdate(download)
    }

    override fun onCancelled(download: Download) {
        cancelNotification(download)
    }

    override fun onRemoved(download: Download) {
        cancelNotification(download)
    }

    override fun onDeleted(download: Download) {
        cancelNotification(download)
    }

    override fun onAdded(download: Download) {
        //Not implemented
    }

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
        //Not implemented
    }

    override fun onWaitingNetwork(download: Download) {
        //Not implemented
    }

    override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {
        //Not implemented
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