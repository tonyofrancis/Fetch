package com.tonyodev.fetch2notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tonyodev.fetch2.*

class FetchNotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            val fetchNamespace = intent.getStringExtra(EXTRA_FETCH_NAMESPACE)
            val downloadId = intent.getIntExtra(EXTRA_DOWNLOAD_ID, DOWNLOAD_ID_INVALID)
            val actionType = intent.getIntExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_INVALID)
            val downloadStatus = Status.valueOf(intent.getIntExtra(EXTRA_DOWNLOAD_STATUS, Status.NONE.value))
            when (intent.action) {
                ACTION_NOTIFICATION_ACTION -> {
                    if (!fetchNamespace.isNullOrEmpty() && downloadId != DOWNLOAD_ID_INVALID && actionType != ACTION_TYPE_INVALID) {
                        processNotificationAction(context, downloadId, fetchNamespace, actionType)
                    }
                }
                ACTION_NOTIFICATION_CHECK -> {
                    when (downloadStatus) {
                        Status.QUEUED,
                        Status.DOWNLOADING -> {
                            if (downloadId != DOWNLOAD_ID_INVALID) {
                                if (actionType == ACTION_TYPE_PAUSE && !fetchNamespace.isNullOrEmpty()) {
                                    processNotificationAction(context, downloadId, fetchNamespace, actionType)
                                }
                                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                notificationManager.cancel(downloadId)
                            }
                        }
                        else -> {

                        }
                    }
                }
            }
        }
    }

    private fun processNotificationAction(context: Context, downloadId: Int, fetchNamespace: String, actionType: Int) {
        val fetchConfiguration = FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(0)
                .enableAutoStart(false)
                .setNamespace(fetchNamespace)
                .build()
        val fetch = Fetch.getInstance(fetchConfiguration)
        when (actionType) {
            ACTION_TYPE_CANCEL -> fetch.cancel(downloadId)
            ACTION_TYPE_DELETE -> fetch.delete(downloadId)
            ACTION_TYPE_PAUSE -> fetch.pause(downloadId)
            ACTION_TYPE_RESUME -> fetch.resume(downloadId)
            else -> {
                //do nothing
            }
        }
        fetch.close()
    }

}