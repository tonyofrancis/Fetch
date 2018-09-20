package com.tonyodev.fetch2.fetch

import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.database.DatabaseManager
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.helper.PriorityListProcessor
import com.tonyodev.fetch2.util.*
import com.tonyodev.fetch2core.*
import java.io.File
import java.util.*

/**
 * This handlerWrapper class handles all tasks and operations of Fetch.
 * */
class FetchHandlerImpl(private val namespace: String,
                       private val databaseManager: DatabaseManager,
                       private val downloadManager: DownloadManager,
                       private val priorityListProcessor: PriorityListProcessor<Download>,
                       private val logger: Logger,
                       private val autoStart: Boolean,
                       private val httpDownloader: Downloader,
                       private val fileServerDownloader: FileServerDownloader,
                       private val listenerCoordinator: ListenerCoordinator,
                       private val uiHandler: Handler) : FetchHandler {

    private val listenerId = UUID.randomUUID().hashCode()
    private val listenerSet = mutableSetOf<FetchListener>()

    @Volatile
    private var isTerminating = false

    override fun init() {
        databaseManager.sanitizeOnFirstEntry()
        if (autoStart) {
            priorityListProcessor.start()
        }
    }

    override fun enqueue(request: Request): Download {
        return enqueueRequests(listOf(request)).first()
    }

    override fun enqueue(requests: List<Request>): List<Download> {
        return enqueueRequests(requests)
    }

    private fun enqueueRequests(requests: List<Request>): List<Download> {
        val results = requests.distinctBy { it.file }
                .map {
                    val downloadInfo = it.toDownloadInfo()
                    downloadInfo.namespace = namespace
                    prepareDownloadInfoForEnqueue(downloadInfo)
                    downloadInfo.status = if (it.downloadOnEnqueue) {
                        Status.QUEUED
                    } else {
                        Status.ADDED
                    }
                    val downloadPair = databaseManager.insert(downloadInfo)
                    logger.d("Enqueued download ${downloadPair.first}")
                    downloadPair.first
                }
        startPriorityQueueIfNotStarted()
        return results
    }

    private fun prepareDownloadInfoForEnqueue(downloadInfo: DownloadInfo) {
        val existingDownload = databaseManager.getByFile(downloadInfo.file)
        if (existingDownload == null) {
            createFileIfPossible(File(downloadInfo.file))
        }
        if (downloadInfo.enqueueAction == EnqueueAction.DO_NOT_ENQUEUE_IF_EXISTING && existingDownload != null) {
            throw FetchException(REQUEST_WITH_FILE_PATH_ALREADY_EXIST)
        } else if (downloadInfo.enqueueAction == EnqueueAction.REPLACE_EXISTING && existingDownload != null) {
            deleteDownloads(listOf(downloadInfo.id))
        } else if (downloadInfo.enqueueAction == EnqueueAction.INCREMENT_FILE_NAME && existingDownload != null) {
            val file = getIncrementedFileIfOriginalExists(downloadInfo.file)
            downloadInfo.file = file.absolutePath
            downloadInfo.id = getUniqueId(downloadInfo.url, downloadInfo.file)
            createFileIfPossible(file)
        }
    }

    override fun enqueueCompletedDownload(completedDownload: CompletedDownload): Download {
        return enqueueCompletedDownloads(listOf(completedDownload)).first()
    }

    override fun enqueueCompletedDownloads(completedDownloads: List<CompletedDownload>): List<Download> {
        return completedDownloads.map {
            val downloadInfo = it.toDownloadInfo()
            downloadInfo.namespace = namespace
            downloadInfo.status = Status.COMPLETED
            prepareCompletedDownloadInfoForEnqueue(downloadInfo)
            val downloadPair = databaseManager.insert(downloadInfo)
            logger.d("Enqueued CompletedDownload ${downloadPair.first}")
            downloadPair.first
        }
    }

    private fun prepareCompletedDownloadInfoForEnqueue(downloadInfo: DownloadInfo) {
        val existingDownload = databaseManager.getByFile(downloadInfo.file)
        if (existingDownload != null) {
            deleteDownloads(listOf(downloadInfo.id))
        }
    }

    override fun pause(ids: List<Int>): List<Download> {
        return pauseDownloads(ids)
    }

    override fun pausedGroup(id: Int): List<Download> {
        return pauseDownloads(databaseManager.getByGroup(id).map { it.id })
    }

    private fun pauseDownloads(downloadIds: List<Int>): List<Download> {
        cancelDownloadsIfDownloading(downloadIds)
        val downloads = databaseManager.get(downloadIds).filterNotNull()
        val pausedDownloads = mutableListOf<DownloadInfo>()
        downloads.forEach {
            if (canPauseDownload(it)) {
                it.status = Status.PAUSED
                pausedDownloads.add(it)
            }
        }
        databaseManager.update(pausedDownloads)
        return pausedDownloads
    }

    override fun freeze() {
        priorityListProcessor.pause()
        downloadManager.cancelAll()
    }

    override fun unfreeze() {
        priorityListProcessor.resume()
    }

    override fun resume(ids: List<Int>): List<Download> {
        return resumeDownloads(ids)
    }

    override fun resumeGroup(id: Int): List<Download> {
        return resumeDownloads(databaseManager.getByGroup(id).map { it.id })
    }

    private fun resumeDownloads(downloadIds: List<Int>): List<Download> {
        startPriorityQueueIfNotStarted()
        val downloads = databaseManager.get(downloadIds).filterNotNull()
        val resumedDownloads = mutableListOf<DownloadInfo>()
        downloads.forEach {
            if (!downloadManager.contains(it.id) && canResumeDownload(it)) {
                it.status = Status.QUEUED
                resumedDownloads.add(it)
            }
        }
        databaseManager.update(resumedDownloads)
        return resumedDownloads
    }

    override fun remove(ids: List<Int>): List<Download> {
        return removeDownloads(ids)
    }

    override fun removeGroup(id: Int): List<Download> {
        return removeDownloads(databaseManager.getByGroup(id).map { it.id })
    }

    override fun removeAll(): List<Download> {
        return removeDownloads(databaseManager.get().map { it.id })
    }

    override fun removeAllWithStatus(status: Status): List<Download> {
        return removeDownloads(databaseManager.getByStatus(status).map { it.id })
    }

    override fun removeAllInGroupWithStatus(groupId: Int, status: Status): List<Download> {
        return removeDownloads(databaseManager.getDownloadsInGroupWithStatus(groupId, status).map { it.id })
    }

    private fun removeDownloads(downloadIds: List<Int>): List<Download> {
        cancelDownloadsIfDownloading(downloadIds)
        val downloads = databaseManager.get(downloadIds).filterNotNull()
        databaseManager.delete(downloads)
        downloads.forEach {
            it.status = Status.REMOVED
            databaseManager.delegate?.deleteTempFilesForDownload(it)
        }
        return downloads
    }

    override fun delete(ids: List<Int>): List<Download> {
        return deleteDownloads(ids)
    }

    override fun deleteGroup(id: Int): List<Download> {
        return deleteDownloads(databaseManager.getByGroup(id).map { it.id })
    }

    override fun deleteAll(): List<Download> {
        return deleteDownloads(databaseManager.get().map { it.id })
    }

    override fun deleteAllWithStatus(status: Status): List<Download> {
        return deleteDownloads(databaseManager.getByStatus(status).map { it.id })
    }

    override fun deleteAllInGroupWithStatus(groupId: Int, status: Status): List<Download> {
        return deleteDownloads(databaseManager.getDownloadsInGroupWithStatus(groupId, status).map { it.id })
    }

    private fun deleteDownloads(downloadIds: List<Int>): List<Download> {
        cancelDownloadsIfDownloading(downloadIds)
        val downloads = databaseManager.get(downloadIds).filterNotNull()
        databaseManager.delete(downloads)
        downloads.forEach {
            it.status = Status.DELETED
            try {
                val file = File(it.file)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                logger.d("Failed to delete file ${it.file}", e)
            }
            databaseManager.delegate?.deleteTempFilesForDownload(it)
        }
        return downloads
    }

    override fun cancel(ids: List<Int>): List<Download> {
        return cancelDownloads(ids)
    }

    override fun cancelGroup(id: Int): List<Download> {
        return cancelDownloads(databaseManager.getByGroup(id).map { it.id })
    }

    override fun cancelAll(): List<Download> {
        return cancelDownloads(databaseManager.get().map { it.id })
    }

    private fun cancelDownloads(downloadIds: List<Int>): List<Download> {
        cancelDownloadsIfDownloading(downloadIds)
        val downloads = databaseManager.get(downloadIds).filterNotNull()
        val cancelledDownloads = mutableListOf<DownloadInfo>()
        downloads.forEach {
            if (canCancelDownload(it)) {
                it.status = Status.CANCELLED
                it.error = defaultNoError
                cancelledDownloads.add(it)
            }
        }
        databaseManager.update(cancelledDownloads)
        return cancelledDownloads
    }

    override fun retry(ids: List<Int>): List<Download> {
        startPriorityQueueIfNotStarted()
        val downloadInfoList = databaseManager.get(ids).filterNotNull()
        val retryDownloads = mutableListOf<DownloadInfo>()
        downloadInfoList.forEach {
            if (canRetryDownload(it)) {
                it.status = Status.QUEUED
                it.error = defaultNoError
                retryDownloads.add(it)
            }
        }
        databaseManager.update(retryDownloads)
        return retryDownloads
    }

    override fun updateRequest(requestId: Int, newRequest: Request): Download {
        val oldDownloadInfo = databaseManager.get(requestId)
        if (oldDownloadInfo != null) {
            val newDownloadInfo = newRequest.toDownloadInfo()
            newDownloadInfo.status = if (newRequest.downloadOnEnqueue) {
                Status.QUEUED
            } else {
                Status.ADDED
            }
            newDownloadInfo.namespace = namespace
            val enqueueAction = newDownloadInfo.enqueueAction
            if (enqueueAction == EnqueueAction.REPLACE_EXISTING) {
                if (newRequest.file == oldDownloadInfo.file) {
                    newDownloadInfo.downloaded = oldDownloadInfo.downloaded
                    newDownloadInfo.total = oldDownloadInfo.total
                }
                remove(listOf(requestId))
            } else {
                delete(listOf(requestId))
            }
            prepareDownloadInfoForEnqueue(newDownloadInfo)
            startPriorityQueueIfNotStarted()
            databaseManager.insert(newDownloadInfo)
            return newDownloadInfo
        } else {
            throw FetchException(REQUEST_DOES_NOT_EXIST)
        }
    }

    override fun replaceExtras(id: Int, extras: Extras): Download {
        cancelDownloadsIfDownloading(listOf(id))
        val downloadInfo = databaseManager.get(id)
        return if (downloadInfo != null) {
            val download = databaseManager.updateExtras(id, extras)
            download ?: throw FetchException(REQUEST_DOES_NOT_EXIST)
        } else {
            throw FetchException(REQUEST_DOES_NOT_EXIST)
        }
    }

    override fun getDownloads(): List<Download> {
        return databaseManager.get()
    }

    override fun getDownload(id: Int): Download? {
        return databaseManager.get(id)
    }

    override fun getDownloads(idList: List<Int>): List<Download> {
        return databaseManager.get(idList).filterNotNull()
    }

    override fun getDownloadsInGroup(id: Int): List<Download> {
        return databaseManager.getByGroup(id)
    }

    override fun getDownloadsWithStatus(status: Status): List<Download> {
        return databaseManager.getByStatus(status)
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): List<Download> {
        return databaseManager.getDownloadsInGroupWithStatus(groupId, status)
    }

    override fun getDownloadsByRequestIdentifier(identifier: Long): List<Download> {
        return databaseManager.getDownloadsByRequestIdentifier(identifier)
    }

    override fun getDownloadBlocks(id: Int): List<DownloadBlock> {
        val download = databaseManager.get(id)
        return if (download != null) {
            val fileTempDir = downloadManager.getDownloadFileTempDir(download)
            val fileSliceInfo = getFileSliceInfo(getPreviousSliceCount(download.id, fileTempDir), download.total)
            when {
                download.total < 1 -> listOf()
                fileSliceInfo.slicingCount < 2 -> {
                    val downloadBlockInfo = DownloadBlockInfo()
                    downloadBlockInfo.downloadId = download.id
                    downloadBlockInfo.blockPosition = 1
                    downloadBlockInfo.startByte = 0
                    downloadBlockInfo.endByte = download.total
                    downloadBlockInfo.downloadedBytes = download.downloaded
                    listOf(downloadBlockInfo)
                }
                else -> {
                    var counterBytes = 0L
                    val downloadBlocksList = mutableListOf<DownloadBlockInfo>()
                    for (position in 1..fileSliceInfo.slicingCount) {
                        val startBytes = counterBytes
                        val endBytes = if (fileSliceInfo.slicingCount == position) {
                            download.total
                        } else {
                            counterBytes + fileSliceInfo.bytesPerFileSlice
                        }
                        counterBytes = endBytes
                        val downloadBlockInfo = DownloadBlockInfo()
                        downloadBlockInfo.downloadId = download.id
                        downloadBlockInfo.blockPosition = position
                        downloadBlockInfo.startByte = startBytes
                        downloadBlockInfo.endByte = endBytes
                        downloadBlockInfo.downloadedBytes = getSavedDownloadedInfo(download.id, position, fileTempDir)
                        downloadBlocksList.add(downloadBlockInfo)
                    }
                    downloadBlocksList
                }
            }
        } else {
            return emptyList()
        }
    }

    override fun getContentLengthForRequest(request: Request, fromServer: Boolean): Long {
        val download = databaseManager.get(request.id)
        if (download != null && download.total > 0) {
            return download.total
        }
        return if (fromServer) {
            if (isFetchFileServerUrl(request.url)) {
                fileServerDownloader.getRequestContentLength(getServerRequestFromRequest(request))
            } else {
                httpDownloader.getRequestContentLength(getServerRequestFromRequest(request))
            }
        } else {
            -1L
        }
    }

    override fun getFetchFileServerCatalog(request: Request): List<FileResource> {
        return fileServerDownloader.getFetchFileServerCatalog(getCatalogServerRequestFromRequest(request))
    }

    override fun close() {
        if (isTerminating) {
            return
        }
        isTerminating = true
        listenerSet.iterator().forEach {
            listenerCoordinator.removeListener(listenerId, it)
        }
        listenerSet.clear()
        priorityListProcessor.stop()
        downloadManager.close()
        FetchModulesBuilder.removeNamespaceInstanceReference(namespace)
    }

    override fun setGlobalNetworkType(networkType: NetworkType) {
        priorityListProcessor.stop()
        priorityListProcessor.globalNetworkType = networkType
        val ids = downloadManager.getActiveDownloadsIds()
        cancelDownloadsIfDownloading(ids)
        val downloads = databaseManager.get(ids).filterNotNull()
        downloads.forEach {
            if (it.status == Status.DOWNLOADING) {
                it.status = Status.QUEUED
                it.error = defaultNoError
            }
        }
        databaseManager.update(downloads)
        priorityListProcessor.start()
    }

    override fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int) {
        priorityListProcessor.stop()
        val ids = downloadManager.getActiveDownloadsIds()
        cancelDownloadsIfDownloading(ids)
        downloadManager.concurrentLimit = downloadConcurrentLimit
        priorityListProcessor.downloadConcurrentLimit = downloadConcurrentLimit
        val downloads = databaseManager.get(ids).filterNotNull()
        downloads.forEach {
            if (it.status == Status.DOWNLOADING) {
                it.status = Status.QUEUED
                it.error = defaultNoError
            }
        }
        databaseManager.update(downloads)
        priorityListProcessor.start()
    }

    override fun enableLogging(enabled: Boolean) {
        logger.d("Enable logging - $enabled")
        logger.enabled = enabled
    }

    override fun addListener(listener: FetchListener, notify: Boolean, autoStart: Boolean) {
        listenerSet.add(listener)
        listenerCoordinator.addListener(listenerId, listener)
        if (notify) {
            val downloads = databaseManager.get()
            downloads.forEach {
                uiHandler.post {
                    when (it.status) {
                        Status.COMPLETED -> {
                            listener.onCompleted(it)
                        }
                        Status.FAILED -> {
                            listener.onError(it, it.error, null)
                        }
                        Status.CANCELLED -> {
                            listener.onCancelled(it)
                        }
                        Status.DELETED -> {
                            listener.onDeleted(it)
                        }
                        Status.PAUSED -> {
                            listener.onPaused(it)
                        }
                        Status.QUEUED -> {
                            listener.onQueued(it, false)
                        }
                        Status.REMOVED -> {
                            listener.onRemoved(it)
                        }
                        Status.DOWNLOADING -> {
                        }
                        Status.ADDED -> {
                            listener.onAdded(it)
                        }
                        Status.NONE -> {
                        }
                    }
                }
            }
        }
        logger.d("Added listener $listener")
        if (autoStart) {
            startPriorityQueueIfNotStarted()
        }
    }

    override fun removeListener(listener: FetchListener) {
        val iterator = listenerSet.iterator()
        while (iterator.hasNext()) {
            val fetchListener = iterator.next()
            if (fetchListener == listener) {
                iterator.remove()
                logger.d("Removed listener $listener")
                break
            }
        }
        listenerCoordinator.removeListener(listenerId, listener)
    }

    private fun cancelDownloadsIfDownloading(ids: List<Int>) {
        ids.forEach { id ->
            if (downloadManager.contains(id)) {
                downloadManager.cancel(id)
            }
        }
    }

    private fun startPriorityQueueIfNotStarted() {
        priorityListProcessor.resetBackOffTime()
        if (priorityListProcessor.isStopped && !isTerminating) {
            priorityListProcessor.start()
        }
        if (priorityListProcessor.isPaused && !isTerminating) {
            priorityListProcessor.resume()
        }
    }

}