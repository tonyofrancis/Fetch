package com.tonyodev.fetch2.fetch

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.FileResource
import java.io.Closeable

/**
 * This handlerWrapper class handles all tasks and operations of Fetch.
 * */
interface FetchHandler : Closeable {

    fun init()
    fun enqueue(request: Request): Download
    fun enqueue(requests: List<Request>): List<Download>
    fun enqueueCompletedDownload(completedDownload: CompletedDownload): Download
    fun enqueueCompletedDownloads(completedDownloads: List<CompletedDownload>): List<Download>
    fun pause(ids: List<Int>): List<Download>
    fun pausedGroup(id: Int): List<Download>
    fun freeze()
    fun unfreeze()
    fun resume(ids: List<Int>): List<Download>
    fun resumeGroup(id: Int): List<Download>
    fun remove(ids: List<Int>): List<Download>
    fun removeGroup(id: Int): List<Download>
    fun removeAll(): List<Download>
    fun removeAllWithStatus(status: Status): List<Download>
    fun removeAllInGroupWithStatus(groupId: Int, status: Status): List<Download>
    fun delete(ids: List<Int>): List<Download>
    fun deleteGroup(id: Int): List<Download>
    fun deleteAll(): List<Download>
    fun deleteAllWithStatus(status: Status): List<Download>
    fun deleteAllInGroupWithStatus(groupId: Int, status: Status): List<Download>
    fun cancel(ids: List<Int>): List<Download>
    fun cancelGroup(id: Int): List<Download>
    fun cancelAll(): List<Download>
    fun retry(ids: List<Int>): List<Download>
    fun updateRequest(requestId: Int, newRequest: Request): Download
    fun getDownloads(): List<Download>
    fun getDownload(id: Int): Download?
    fun getDownloads(idList: List<Int>): List<Download>
    fun getDownloadsInGroup(id: Int): List<Download>
    fun getDownloadsWithStatus(status: Status): List<Download>
    fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): List<Download>
    fun getDownloadsByRequestIdentifier(identifier: Long): List<Download>
    fun setGlobalNetworkType(networkType: NetworkType)
    fun enableLogging(enabled: Boolean)
    fun addListener(listener: FetchListener, notify: Boolean, autoStart: Boolean)
    fun removeListener(listener: FetchListener)
    fun getDownloadBlocks(id: Int): List<DownloadBlock>
    fun getContentLengthForRequest(request: Request, fromServer: Boolean): Long
    fun getFetchFileServerCatalog(request: Request): List<FileResource>
    fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int)
    fun replaceExtras(id: Int, extras: Extras): Download

}