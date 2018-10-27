package com.tonyodev.fetch2notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tonyodev.fetch2.*

class FetchNotificationActionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            when (intent.action) {
                ACTION_NOTIFICATION_ACTION -> {
                    val fetchNamespace = intent.getStringExtra(EXTRA_FETCH_NAMESPACE)
                    val downloadId = intent.getIntExtra(EXTRA_DOWNLOAD_ID, DOWNLOAD_ID_INVALID)
                    val actionType = intent.getIntExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_INVALID)
                    if (!fetchNamespace.isNullOrEmpty() && downloadId != DOWNLOAD_ID_INVALID && actionType != ACTION_TYPE_INVALID) {
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
            }
        }
    }

}