package com.tonyodev.fetch2.provider

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchListener
import java.util.Collections


class ListenerProvider {

    val listeners: MutableSet<FetchListener> =
            Collections.synchronizedSet(mutableSetOf<FetchListener>())

    val mainListener: FetchListener = object : FetchListener {

        override fun onQueued(download: Download) {
            listeners.iterator().forEach {
                it.onQueued(download)
            }
        }

        override fun onCompleted(download: Download) {
            listeners.iterator().forEach {
                it.onCompleted(download)
            }
        }

        override fun onError(download: Download) {
            listeners.iterator().forEach {
                it.onError(download)
            }
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            listeners.iterator().forEach {
                it.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
            }
        }

        override fun onPaused(download: Download) {
            listeners.iterator().forEach {
                it.onPaused(download)
            }
        }

        override fun onResumed(download: Download) {
            listeners.iterator().forEach {
                it.onResumed(download)
            }
        }

        override fun onCancelled(download: Download) {
            listeners.iterator().forEach {
                it.onCancelled(download)
            }
        }

        override fun onRemoved(download: Download) {
            listeners.iterator().forEach {
                it.onRemoved(download)
            }
        }

        override fun onDeleted(download: Download) {
            listeners.iterator().forEach {
                it.onDeleted(download)
            }
        }
    }

}