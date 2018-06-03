package com.tonyodev.fetch2.fetch

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchListener

class ListenerCoordinator(val namespace: String) {

    private val lock = Any()
    private val listenerMap = mutableMapOf<Int, MutableSet<FetchListener>>()

    fun addListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val set = listenerMap[id] ?: mutableSetOf()
            set.add(fetchListener)
            listenerMap[id] = set
        }
    }

    fun removeListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val iterator = listenerMap[id]?.iterator()
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
            listenerMap.values.iterator().forEach {
                it.forEach { onQueued(download) }
            }
        }

        override fun onCompleted(download: Download) {
            listenerMap.values.iterator().forEach {
                it.forEach { onCompleted(download) }
            }
        }

        override fun onError(download: Download) {
            listenerMap.values.iterator().forEach {
                it.forEach { onError(download) }
            }
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            listenerMap.values.iterator().forEach {
                it.forEach { onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond) }
            }
        }

        override fun onPaused(download: Download) {
            listenerMap.values.iterator().forEach {
                it.forEach { onPaused(download) }
            }
        }

        override fun onResumed(download: Download) {
            listenerMap.values.iterator().forEach {
                it.forEach { onResumed(download) }
            }
        }

        override fun onCancelled(download: Download) {
            listenerMap.values.iterator().forEach {
                it.forEach { onCancelled(download) }
            }
        }

        override fun onRemoved(download: Download) {
            listenerMap.values.iterator().forEach {
                it.forEach { onRemoved(download) }
            }
        }

        override fun onDeleted(download: Download) {
            listenerMap.values.iterator().forEach {
                it.forEach { onDeleted(download) }
            }
        }
    }

    fun clearAll() {
        listenerMap.clear()
    }

}