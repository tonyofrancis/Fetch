package com.tonyodev.fetch2

import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
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
        val notificationBuilder = NotificationCompat.Builder(context, defaultNotificationChannel)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.round_save_alt_white_24)
                .setContentTitle(download.url)
                .setContentText("Downloading")
                .setProgress(100, download.progress, download.total == -1L)
        return notificationBuilder.build()
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

    override fun onAdded(download: Download) {
        postNotificationUpdate(download)
    }

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
        postNotificationUpdate(download)
    }

    override fun onWaitingNetwork(download: Download) {
        postNotificationUpdate(download)
    }

    override fun onCompleted(download: Download) {
        postNotificationUpdate(download)
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        postNotificationUpdate(download)
    }

    override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {
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

}