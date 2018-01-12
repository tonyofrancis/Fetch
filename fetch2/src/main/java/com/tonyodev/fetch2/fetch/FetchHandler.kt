package com.tonyodev.fetch2.fetch

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.provider.ListenerProvider
import java.io.Closeable

/**
 * This handler class handles all tasks and operations of Fetch.
 * */
interface FetchHandler : Closeable {

    val isClosed: Boolean
    val fetchListenerProvider: ListenerProvider

    fun init()
    fun enqueue(request: Request): Download
    fun enqueue(requests: List<Request>): List<Download>
    fun pause(ids: IntArray): List<Download>
    fun pausedGroup(id: Int): List<Download>
    fun freeze()
    fun unfreeze()
    fun resume(ids: IntArray): List<Download>
    fun resumeGroup(id: Int): List<Download>
    fun remove(ids: IntArray): List<Download>
    fun removeGroup(id: Int): List<Download>
    fun removeAll(): List<Download>
    fun delete(ids: IntArray): List<Download>
    fun deleteGroup(id: Int): List<Download>
    fun deleteAll(): List<Download>
    fun cancel(ids: IntArray): List<Download>
    fun cancelGroup(id: Int): List<Download>
    fun cancelAll(): List<Download>
    fun retry(ids: IntArray): List<Download>
    fun updateRequest(id: Int, requestInfo: RequestInfo): Download?
    fun getDownloads(): List<Download>
    fun getDownload(id: Int): Download?
    fun getDownloads(idList: List<Int>): List<Download>
    fun getDownloadsInGroup(id: Int): List<Download>
    fun getDownloadsWithStatus(status: Status): List<Download>
    fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): List<Download>
    fun setGlobalNetworkType(networkType: NetworkType)
    fun enableLogging(enabled: Boolean)
    fun addListener(listener: FetchListener)
    fun removeListener(listener: FetchListener)
    fun isDownloading(id: Int): Boolean
    fun cancelDownload(id: Int): Boolean
    fun throwExceptionIfClosed()

}