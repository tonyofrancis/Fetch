package com.tonyodev.fetch2.fetch

import android.os.Handler
import android.os.HandlerThread
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.provider.GroupInfoProvider
import com.tonyodev.fetch2core.DownloadBlock
import java.lang.ref.WeakReference

class ListenerCoordinator(val namespace: String,
                          private val groupInfoProvider: GroupInfoProvider,
                          private val uiHandler: Handler) {

    private val lock = Any()
    private val fetchListenerMap = mutableMapOf<Int, MutableSet<WeakReference<FetchListener>>>()
    private val fetchGroupListenerMap = mutableMapOf<Int, MutableSet<WeakReference<FetchGroupListener>>>()
    private val fetchNotificationManagerList = mutableListOf<FetchNotificationManager>()
    private val fetchNotificationHandler = {
        val handlerThread = HandlerThread("FetchNotificationsIO")
        handlerThread.start()
        Handler(handlerThread.looper)
    }()

    fun addListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val set = fetchListenerMap[id] ?: mutableSetOf()
            set.add(WeakReference(fetchListener))
            fetchListenerMap[id] = set
            if (fetchListener is FetchGroupListener) {
                val groupSet = fetchGroupListenerMap[id] ?: mutableSetOf()
                groupSet.add(WeakReference(fetchListener))
                fetchGroupListenerMap[id] = groupSet
            }
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
            if (fetchListener is FetchGroupListener) {
                val groupIterator = fetchGroupListenerMap[id]?.iterator()
                if (groupIterator != null) {
                    while (groupIterator.hasNext()) {
                        val reference = groupIterator.next()
                        if (reference.get() == fetchListener) {
                            groupIterator.remove()
                            break
                        }
                    }
                }
            }
        }
    }

    fun addNotificationManager(fetchNotificationManager: FetchNotificationManager) {
        synchronized(lock) {
            if (!fetchNotificationManagerList.contains(fetchNotificationManager)) {
                fetchNotificationManagerList.add(fetchNotificationManager)
            }
        }
    }

    fun removeNotificationManager(fetchNotificationManager: FetchNotificationManager) {
        synchronized(lock) {
            fetchNotificationManagerList.remove(fetchNotificationManager)
        }
    }

    fun cancelOnGoingNotifications(fetchNotificationManager: FetchNotificationManager) {
        synchronized(lock) {
            fetchNotificationHandler.post {
                synchronized(lock) {
                    fetchNotificationManager.cancelOngoingNotifications()
                }
            }
        }
    }

    val mainListener: FetchListener = object : FetchListener {

        override fun onAdded(download: Download) {
            synchronized(lock) {
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onAdded(download)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                uiHandler.post {
                                    fetchListener.onAdded(groupId, download, fetchGroup)
                                }
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            synchronized(lock) {
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onQueued(download, waitingOnNetwork)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onQueued(groupId, download, waitingOnNetwork, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onWaitingNetwork(download: Download) {
            synchronized(lock) {
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onWaitingNetwork(download)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onWaitingNetwork(groupId, download, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onCompleted(download: Download) {
            synchronized(lock) {
                fetchNotificationHandler.post {
                    synchronized(lock) {
                        for (fetchNotificationManager in fetchNotificationManagerList) {
                            if (fetchNotificationManager.postNotificationUpdate(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onCompleted(download)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onCompleted(groupId, download, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            synchronized(lock) {
                fetchNotificationHandler.post {
                    synchronized(lock) {
                        for (fetchNotificationManager in fetchNotificationManagerList) {
                            if (fetchNotificationManager.postNotificationUpdate(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onError(download, error, throwable)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onError(groupId, download, error, throwable, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {
            synchronized(lock) {
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            fetchListener.onDownloadBlockUpdated(download, downloadBlock, totalBlocks)
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onDownloadBlockUpdated(groupId, download, downloadBlock, totalBlocks, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
            synchronized(lock) {
                fetchNotificationHandler.post {
                    synchronized(lock) {
                        for (fetchNotificationManager in fetchNotificationManagerList) {
                            if (fetchNotificationManager.postNotificationUpdate(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onStarted(download, downloadBlocks, totalBlocks)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onStarted(groupId, download, downloadBlocks, totalBlocks, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            synchronized(lock) {
                fetchNotificationHandler.post {
                    synchronized(lock) {
                        for (fetchNotificationManager in fetchNotificationManagerList) {
                            if (fetchNotificationManager.postNotificationUpdate(download, etaInMilliSeconds, downloadedBytesPerSecond)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onProgress(groupId, download, etaInMilliSeconds, downloadedBytesPerSecond, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onPaused(download: Download) {
            synchronized(lock) {
                fetchNotificationHandler.post {
                    synchronized(lock) {
                        for (fetchNotificationManager in fetchNotificationManagerList) {
                            if (fetchNotificationManager.postNotificationUpdate(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onPaused(download)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onPaused(groupId, download, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onResumed(download: Download) {
            synchronized(lock) {
                fetchNotificationHandler.post {
                    synchronized(lock) {
                        for (fetchNotificationManager in fetchNotificationManagerList) {
                            if (fetchNotificationManager.postNotificationUpdate(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onResumed(download)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onResumed(groupId, download, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onCancelled(download: Download) {
            synchronized(lock) {
                fetchNotificationHandler.post {
                    synchronized(lock) {
                        for (fetchNotificationManager in fetchNotificationManagerList) {
                            if (fetchNotificationManager.postNotificationUpdate(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onCancelled(download)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onCancelled(groupId, download, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onRemoved(download: Download) {
            synchronized(lock) {
                fetchNotificationHandler.post {
                    synchronized(lock) {
                        for (fetchNotificationManager in fetchNotificationManagerList) {
                            if (fetchNotificationManager.postNotificationUpdate(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onRemoved(download)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onRemoved(groupId, download, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }

        override fun onDeleted(download: Download) {
            synchronized(lock) {
                fetchNotificationHandler.post {
                    synchronized(lock) {
                        for (fetchNotificationManager in fetchNotificationManagerList) {
                            if (fetchNotificationManager.postNotificationUpdate(download)) break
                        }
                    }
                }
                fetchListenerMap.values.forEach {
                    val iterator = it.iterator()
                    while (iterator.hasNext()) {
                        val fetchListener = iterator.next().get()
                        if (fetchListener == null) {
                            iterator.remove()
                        } else {
                            uiHandler.post {
                                fetchListener.onDeleted(download)
                            }
                        }
                    }
                }
                if (fetchGroupListenerMap.isNotEmpty()) {
                    val groupId = download.group
                    val fetchGroup = groupInfoProvider.getGroupReplace(groupId, download)
                    fetchGroupListenerMap.values.forEach {
                        val iterator = it.iterator()
                        while (iterator.hasNext()) {
                            val fetchListener = iterator.next().get()
                            if (fetchListener == null) {
                                iterator.remove()
                            } else {
                                fetchListener.onDeleted(groupId, download, fetchGroup)
                            }
                        }
                    }
                } else {
                    groupInfoProvider.postGroupReplace(download.group, download)
                }
            }
        }
    }

    fun clearAll() {
        synchronized(lock) {
            fetchListenerMap.clear()
            fetchGroupListenerMap.clear()
            fetchNotificationManagerList.clear()
        }
    }

}