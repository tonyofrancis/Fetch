package com.tonyodev.fetch2notifications

import android.app.Notification
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.NotificationManager
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.DownloadBlock

open class DefaultNotificationManager(protected val context: Context,
                                      protected val defaultNotificationChannel: String) : NotificationManager {

    protected val activeNotificationSet = mutableSetOf<Int>()
    protected val notificationManager = NotificationManagerCompat.from(context)
    protected val notificationHandler = {
        val handlerThread = HandlerThread("NotificationIO")
        handlerThread.start()
        Handler(handlerThread.looper)
    }()

    override fun buildNotification(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Notification {
        val progress = if (download.progress < 0) 0 else download.progress
        val progressIndeterminate = download.total == -1L
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.fetch_notification_icon_large)
        val title = getContentTitle(download)
        val contentText = getContentText(context, download, etaInMilliSeconds)
        val notificationBuilder = NotificationCompat.Builder(context, defaultNotificationChannel)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.fetch_notification_icon_small)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(contentText)
                .setProgress(100, progress, progressIndeterminate)
        when (download.status) {
            Status.DOWNLOADING -> {
                notificationBuilder.setOngoing(true)
                        .addAction(R.drawable.fetch_notification_pause, context.getString(R.string.fetch_notification_download_pause), null)
                        .addAction(R.drawable.fetch_notification_cancel, context.getString(R.string.fetch_notification_download_cancel), null)
            }
            Status.PAUSED -> {
                notificationBuilder.setOngoing(true)
                        .addAction(R.drawable.fetch_notification_resume, context.getString(R.string.fetch_notification_download_resume), null)
                        .addAction(R.drawable.fetch_notification_cancel, context.getString(R.string.fetch_notification_download_cancel), null)
            }
            Status.COMPLETED -> {
                notificationBuilder.setOngoing(false)

            }
            Status.FAILED -> {
                notificationBuilder.setOngoing(false)
            }
            else -> {
                //do nothing
            }
        }
        return notificationBuilder.build()
    }

    private fun getContentTitle(download: Download): String {
        return download.fileUri.lastPathSegment ?: Uri.parse(download.url).lastPathSegment
        ?: download.url
    }

    private fun getContentText(context: Context, download: Download, etaInMilliSeconds: Long): String {
        return when {
            download.status == Status.COMPLETED -> context.getString(R.string.fetch_notification_download_complete)
            download.status == Status.FAILED -> context.getString(R.string.fetch_notification_download_failed)
            etaInMilliSeconds < 0 -> ""
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

    override fun getNotification(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long): Notification {
        val notification = buildNotification(download, etaInMilliSeconds, downloadedBytesPerSecond)
        activeNotificationSet.add(download.id)
        return notification
    }

    override fun cancelNotification(download: Download) {
        notificationHandler.post {
            if (activeNotificationSet.contains(download.id)) {
                notificationManager.cancel(download.id)
            }
            activeNotificationSet.remove(download.id)
        }
    }

    override fun postNotificationUpdate(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
        notificationHandler.post {
            notificationManager.notify(download.id, getNotification(download, etaInMilliSeconds, downloadedBytesPerSecond))
        }
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

}