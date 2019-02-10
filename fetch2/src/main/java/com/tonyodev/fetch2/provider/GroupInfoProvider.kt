package com.tonyodev.fetch2.provider

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.model.FetchGroupInfo
import java.lang.ref.WeakReference

class GroupInfoProvider(private val namespace: String,
                        private val downloadProvider: DownloadProvider) {

    private val lock = Any()
    private val groupInfoMap = mutableMapOf<Int, WeakReference<FetchGroupInfo>>()

    private fun getGroupInfo(id: Int): FetchGroupInfo {
        val info = groupInfoMap[id]?.get()
        return if (info == null) {
            val groupInfo = FetchGroupInfo(id, namespace)
            groupInfoMap[id] =  WeakReference(groupInfo)
            groupInfo
        } else {
            info
        }
    }

    fun getGroupReplace(id: Int, download: Download): FetchGroup {
        return synchronized(lock) {
            val groupInfo = getGroupInfo(id)
            groupInfo.downloads = downloadProvider.getByGroupReplace(id, download)
            groupInfo
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