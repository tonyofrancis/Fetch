package com.tonyodev.fetch2.fetch

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NotificationManager
import com.tonyodev.fetch2core.DownloadBlock
import java.lang.ref.WeakReference

class ListenerCoordinator(val namespace: String,
                          val notificationManager: NotificationManager?) {

    private val lock = Any()
    private val listenerMap = mutableMapOf<Int, MutableSet<WeakReference<FetchListener>>>()

    fun addListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val set = listenerMap[id] ?: mutableSetOf()
            set.add(WeakReference(fetchListener))
            listenerMap[id] = set
        }
    }

    fun removeListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val iterator = listenerMap[id]?.iterator()
            if (iterator != null) {
                while (iterator.hasNext()) {
                    val reference = iterator.next()
                    if (reference.get() == fetchListener) {
                        iterator.remove()
                        break
                    }
                }
            }
        }
    }

    val mainListener: FetchListener = object : FetchListener {

        override fun onAdded(download: Download) {
            synchronized(lock) {
                notificationManager?.onAdded(download)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onAdded(download)
                        }
                    }
                }
            }
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            synchronized(lock) {
                notificationManager?.onQueued(download, waitingOnNetwork)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onQueued(download, waitingOnNetwork)
                        }
                    }
                }
            }
        }

        override fun onWaitingNetwork(download: Download) {
            synchronized(lock) {
                notificationManager?.onWaitingNetwork(download)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onWaitingNetwork(download)
                        }
                    }
                }
            }
        }

        override fun onCompleted(download: Download) {
            synchronized(lock) {
                notificationManager?.onCompleted(download)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onCompleted(download)
                        }
                    }
                }
            }
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            synchronized(lock) {
                notificationManager?.onError(download, error, throwable)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onError(download, error, throwable)
                        }
                    }
                }
            }
        }

        override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {
            synchronized(lock) {
                notificationManager?.onDownloadBlockUpdated(download, downloadBlock, totalBlocks)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onDownloadBlockUpdated(download, downloadBlock, totalBlocks)
                        }
                    }
                }
            }
        }

        override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
            synchronized(lock) {
                notificationManager?.onStarted(download, downloadBlocks, totalBlocks)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onStarted(download, downloadBlocks, totalBlocks)
                        }
                    }
                }
            }
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            synchronized(lock) {
                notificationManager?.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
                        }
                    }
                }
            }
        }

        override fun onPaused(download: Download) {
            synchronized(lock) {
                notificationManager?.onPaused(download)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onPaused(download)
                        }
                    }
                }
            }
        }

        override fun onResumed(download: Download) {
            synchronized(lock) {
                notificationManager?.onResumed(download)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onResumed(download)
                        }
                    }
                }
            }
        }

        override fun onCancelled(download: Download) {
            synchronized(lock) {
                notificationManager?.onCancelled(download)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onCancelled(download)
                        }
                    }
                }
            }
        }

        override fun onRemoved(download: Download) {
            synchronized(lock) {
                notificationManager?.onRemoved(download)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onRemoved(download)
                        }
                    }
                }
            }
        }

        override fun onDeleted(download: Download) {
            synchronized(lock) {
                notificationManager?.onDeleted(download)
                listenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val reference = iterator.next()
                        if (reference.get() == null) {
                            iterator.remove()
                        } else {
                            reference.get()?.onDeleted(download)
                        }
                    }
                }
            }
        }
    }

    fun clearAll() {
        synchronized(lock) {
            listenerMap.clear()
        }
    }

}