package com.tonyodev.fetch2.fetch

import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.*
import java.io.Closeable

/**
 * This handlerWrapper class handles all tasks and operations of Fetch.
 * */
interface FetchHandler : Closeable {

    fun init()
    fun enqueue(request: Request): Pair<Download, Error>
    fun enqueue(requests: List<Request>): List<Pair<Download, Error>>
    fun enqueueCompletedDownload(completedDownload: CompletedDownload): Download
    fun enqueueCompletedDownloads(completedDownloads: List<CompletedDownload>): List<Download>
    fun pause(ids: List<Int>): List<Download>
    fun pausedGroup(id: Int): List<Download>
    fun pauseAll(): List<Download>
    fun freeze()
    fun unfreeze()
    fun resume(ids: List<Int>): List<Download>
    fun resumeGroup(id: Int): List<Download>
    fun resumeAll(): List<Download>
    fun remove(ids: List<Int>): List<Download>
    fun removeGroup(id: Int): List<Download>
    fun removeAll(): List<Download>
    fun removeAllWithStatus(status: Status): List<Download>
    fun removeAllInGroupWithStatus(groupId: Int, statuses: List<Status>): List<Download>
    fun delete(ids: List<Int>): List<Download>
    fun deleteGroup(id: Int): List<Download>
    fun deleteAll(): List<Download>
    fun deleteAllWithStatus(status: Status): List<Download>
    fun deleteAllInGroupWithStatus(groupId: Int, statuses: List<Status>): List<Download>
    fun cancel(ids: List<Int>): List<Download>
    fun cancelGroup(id: Int): List<Download>
    fun cancelAll(): List<Download>
    fun retry(ids: List<Int>): List<Download>
    fun updateRequest(requestId: Int, newRequest: Request): Pair<Download, Boolean>
    fun getDownloads(): List<Download>
    fun getDownload(id: Int): Download?
    fun getDownloads(idList: List<Int>): List<Download>
    fun getDownloadsInGroup(id: Int): List<Download>
    fun getDownloadsWithStatus(status: Status): List<Download>
    fun getAllGroupIds(): List<Int>
    fun getDownloadsByTag(tag: String): List<Download>
    fun getDownloadsWithStatus(statuses: List<Status>): List<Download>
    fun getDownloadsInGroupWithStatus(groupId: Int, statuses: List<Status>): List<Download>
    fun getDownloadsByRequestIdentifier(identifier: Long): List<Download>
    fun setGlobalNetworkType(networkType: NetworkType)
    fun enableLogging(enabled: Boolean)
    fun addListener(listener: FetchListener, notify: Boolean, autoStart: Boolean)
    fun removeListener(listener: FetchListener)
    fun getDownloadBlocks(id: Int): List<DownloadBlock>
    fun getContentLengthForRequest(request: Request, fromServer: Boolean): Long
    fun getServerResponse(url: String, header: Map<String, String>? = null): Downloader.Response
    fun getFetchFileServerCatalog(request: Request): List<FileResource>
    fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int)
    fun replaceExtras(id: Int, extras: Extras): Download
    fun hasActiveDownloads(includeAddedDownloads: Boolean): Boolean
    fun getListenerSet(): Set<FetchListener>
    fun getPendingCount(): Long
    fun renameCompletedDownloadFile(id: Int, newFileName: String): Download
    fun getFetchGroup(id: Int): FetchGroup
    fun addFetchObserversForDownload(downloadId: Int, vararg  fetchObservers: FetchObserver<Download>)
    fun removeFetchObserversForDownload(downloadId: Int, vararg fetchObservers: FetchObserver<Download>)
    fun resetAutoRetryAttempts(downloadId: Int, retryDownload: Boolean): Download?
}