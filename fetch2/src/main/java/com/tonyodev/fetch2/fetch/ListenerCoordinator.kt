package com.tonyodev.fetch2.fetch

import android.os.Handler
import android.os.HandlerThread
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NotificationManager
import com.tonyodev.fetch2core.DownloadBlock
import java.lang.ref.WeakReference

class ListenerCoordinator(val namespace: String) {

    private val lock = Any()
    private val fetchListenerMap = mutableMapOf<Int, MutableSet<WeakReference<FetchListener>>>()
    private val notificationManagerList = mutableListOf<NotificationManager>()
    private val notificationHandler = {
        val handlerThread = HandlerThread("FetchNotificationsIO")
        handlerThread.start()
        Handler(handlerThread.looper)
    }()

    fun addListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val set = fetchListenerMap[id] ?: mutableSetOf()
            set.add(WeakReference(fetchListener))
            fetchListenerMap[id] = set
        }
    }

    fun removeListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val iterator = fetchListenerMap[id]?.iterator()
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

    fun addNotificationManager(notificationManager: NotificationManager) {
        synchronized(lock) {
            if (!notificationManagerList.contains(notificationManager)) {
                notificationManagerList.add(notificationManager)
            }
        }
    }

    fun removeNotificationManager(notificationManager: NotificationManager) {
        synchronized(lock) {
            notificationManagerList.remove(notificationManager)
        }
    }

    val mainListener: FetchListener = object : FetchListener {

        override fun onAdded(download: Download) {
            synchronized(lock) {
                fetchListenerMap.values.forEach {
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
                fetchListenerMap.values.forEach {
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
                fetchListenerMap.values.forEach {
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
                notificationHandler.post {
                    synchronized(lock) {
                        for (notificationManager in notificationManagerList) {
                            if (notificationManager.onCompleted(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
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
                notificationHandler.post {
                    synchronized(lock) {
                        for (notificationManager in notificationManagerList) {
                            if (notificationManager.onError(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
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
                fetchListenerMap.values.forEach {
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
                notificationHandler.post {
                    synchronized(lock) {
                        for (notificationManager in notificationManagerList) {
                            if (notificationManager.onStarted(download, downloadBlocks, totalBlocks)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
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
                notificationHandler.post {
                    synchronized(lock) {
                        for (notificationManager in notificationManagerList) {
                            if (notificationManager.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
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
                notificationHandler.post {
                    synchronized(lock) {
                        for (notificationManager in notificationManagerList) {
                            if (notificationManager.onPaused(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
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
                notificationHandler.post {
                    synchronized(lock) {
                        for (notificationManager in notificationManagerList) {
                            if (notificationManager.onResumed(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
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
                notificationHandler.post {
                    synchronized(lock) {
                        for (notificationManager in notificationManagerList) {
                            if (notificationManager.onCancelled(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
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
                notificationHandler.post {
                    synchronized(lock) {
                        for (notificationManager in notificationManagerList) {
                            if (notificationManager.onRemoved(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
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
                notificationHandler.post {
                    synchronized(lock) {
                        for (notificationManager in notificationManagerList) {
                            if (notificationManager.onDeleted(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
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
            fetchListenerMap.clear()
            notificationManagerList.clear()
        }
    }

}