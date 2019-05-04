package com.tonyodev.fetch2.provider

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.model.FetchGroupInfo
import com.tonyodev.fetch2core.Reason
import java.lang.ref.WeakReference

class GroupInfoProvider(private val namespace: String,
                        private val downloadProvider: DownloadProvider) {

    private val lock = Any()
    private val groupInfoMap = mutableMapOf<Int, WeakReference<FetchGroupInfo>>()

    fun getGroupInfo(id: Int, reason: Reason): FetchGroupInfo {
        synchronized(lock) {
            val info = groupInfoMap[id]?.get()
            return if (info == null) {
                val groupInfo = FetchGroupInfo(id, namespace)
                groupInfo.update(downloadProvider.getByGroup(id), null, reason)
                groupInfoMap[id] =  WeakReference(groupInfo)
                groupInfo
            } else {
                info
            }
        }
    }

    fun getGroupReplace(id: Int, download: Download, reason: Reason): FetchGroup {
        return synchronized(lock) {
            val groupInfo = getGroupInfo(id, reason)
            groupInfo.update(downloadProvider.getByGroupReplace(id, download), download, reason)
            groupInfo
        }
    }

    fun postGroupReplace(id: Int, download: Download, reason: Reason) {
        synchronized(lock) {
            val groupInfo = groupInfoMap[id]?.get()
            groupInfo?.update(downloadProvider.getByGroupReplace(id, download), download, reason)
        }
    }

    fun clean() {
        synchronized(lock) {
            val iterator = groupInfoMap.iterator()
            while (iterator.hasNext()) {
                val ref = iterator.next()
                if (ref.value.get() == null) {
                    iterator.remove()
                }
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            groupInfoMap.clear()
        }
    }

}