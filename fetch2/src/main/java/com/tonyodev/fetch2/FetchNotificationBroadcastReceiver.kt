package com.tonyodev.fetch2

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FetchNotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val namespace = intent.getStringExtra(EXTRA_NAMESPACE)
            val downloadId = intent.getIntExtra(EXTRA_DOWNLOAD_ID, DOWNLOAD_ID_INVALID)
            val actionType = intent.getIntExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_INVALID)
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_INVALID)
            when (intent.action) {
                ACTION_NOTIFICATION_ACTION -> {
                    if (!namespace.isNullOrEmpty() && downloadId != DOWNLOAD_ID_INVALID && actionType != ACTION_TYPE_INVALID) {
                        processNotificationAction(context, downloadId, namespace, actionType)
                    }
                }
                ACTION_NOTIFICATION_CHECK -> {
                    if (notificationId != NOTIFICATION_ID_INVALID) {
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(notificationId)
                    }
                }
            }
        }
    }

    private fun processNotificationAction(context: Context, downloadId: Int, namespace: String, actionType: Int) {
        val fetch = getFetchInstance(context, namespace)
        when (actionType) {
            ACTION_TYPE_CANCEL -> fetch.cancel(downloadId)
            ACTION_TYPE_DELETE -> fetch.delete(downloadId)
            ACTION_TYPE_PAUSE -> fetch.pause(downloadId)
            ACTION_TYPE_RESUME -> fetch.resume(downloadId)
            ACTION_TYPE_RETRY -> fetch.retry(downloadId)
            else -> {
                //do nothing
            }
        }
        fetch.close()
    }

    private fun getFetchInstance(context: Context, namespace: String): Fetch {
        val fetchConfiguration = FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(0)
                .enableAutoStart(false)
                .setNamespace(namespace)
                .build()
        return Fetch.getInstance(fetchConfiguration)
    }

}