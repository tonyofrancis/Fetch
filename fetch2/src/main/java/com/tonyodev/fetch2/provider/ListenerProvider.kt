package com.tonyodev.fetch2.provider

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchListener
import java.util.Collections


object ListenerProvider {

    private val lock = Any()
    private val listeners: MutableMap<String, MutableSet<FetchListener>> =
            Collections.synchronizedMap(mutableMapOf<String, MutableSet<FetchListener>>())

    fun addListener(namespace: String, fetchListener: FetchListener) {
        synchronized(lock) {
            val listenerSet = listeners[namespace] ?: mutableSetOf()
            listenerSet.add(fetchListener)
            listeners[namespace] = listenerSet
        }
    }

    fun removeListener(namespace: String, fetchListener: FetchListener) {
        synchronized(lock) {
            val iterator = listeners[namespace]?.iterator()
            if (iterator != null) {
                while (iterator.hasNext()) {
                    val listener = iterator.next()
                    if (listener == fetchListener) {
                        iterator.remove()
                        break
                    }
                }
            }
        }
    }

    val mainListener: FetchListener = object : FetchListener {

        override fun onQueued(download: Download) {
            listeners[download.namespace]?.iterator()?.forEach {
                it.onQueued(download)
            }
        }

        override fun onCompleted(download: Download) {
            listeners[download.namespace]?.iterator()?.forEach {
                it.onCompleted(download)
            }
        }

        override fun onError(download: Download) {
            listeners[download.namespace]?.iterator()?.forEach {
                it.onError(download)
            }
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            listeners[download.namespace]?.iterator()?.forEach {
                it.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
            }
        }

        override fun onPaused(download: Download) {
            listeners[download.namespace]?.iterator()?.forEach {
                it.onPaused(download)
            }
        }

        override fun onResumed(download: Download) {
            listeners[download.namespace]?.iterator()?.forEach {
                it.onResumed(download)
            }
        }

        override fun onCancelled(download: Download) {
            listeners[download.namespace]?.iterator()?.forEach {
                it.onCancelled(download)
            }
        }

        override fun onRemoved(download: Download) {
            listeners[download.namespace]?.iterator()?.forEach {
                it.onRemoved(download)
            }
        }

        override fun onDeleted(download: Download) {
            listeners[download.namespace]?.iterator()?.forEach {
                it.onDeleted(download)
            }
        }
    }

}