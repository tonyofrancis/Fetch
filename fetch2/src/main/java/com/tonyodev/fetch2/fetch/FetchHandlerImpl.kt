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
                       private val fileTempDir: String,
                       private val listenerCoordinator: ListenerCoordinator,
                       private val uiHandler: Handler) : FetchHandler {

    private val listenerId = UUID.randomUUID().hashCode()
    private val listenerSet = mutableSetOf<FetchListener>()

    @Volatile
    private var isTerminating = false

    override fun init() {
        databaseManager.sanitize(true)
        if (autoStart) {
            priorityListProcessor.start()
        }
    }

    override fun enqueue(request: Request): Download {
        val downloadInfo = request.toDownloadInfo()
        downloadInfo.namespace = namespace
        downloadInfo.status = Status.QUEUED
        prepareDownloadInfoForEnqueue(downloadInfo)
        databaseManager.insert(downloadInfo)
        startPriorityQueueIfNotStarted()
        return downloadInfo
    }

    private fun prepareDownloadInfoForEnqueue(downloadInfo: DownloadInfo) {
        val existingDownload = databaseManager.getByFile(downloadInfo.file)
        if (downloadInfo.enqueueAction == EnqueueAction.THROW_ERROR_IF_EXISTING && existingDownload != null) {
            throw FetchException(REQUEST_WITH_FILE_PATH_ALREADY_EXIST, FetchException.Code.REQUEST_WITH_FILE_PATH_ALREADY_EXIST)
        } else if (downloadInfo.enqueueAction == EnqueueAction.REPLACE_EXISTING && existingDownload != null) {
            if (isDownloading(existingDownload.id)) {
                downloadManager.cancel(downloadInfo.id)
            }
            deleteRequestTempFiles(fileTempDir, httpDownloader, existingDownload)
            databaseManager.delete(existingDownload)
        } else if (downloadInfo.enqueueAction == EnqueueAction.INCREMENT_FILE_NAME && existingDownload != null) {
            val file = getIncrementedFileIfOriginalExists(downloadInfo.file)
            downloadInfo.file = file.absolutePath
            downloadInfo.id = getUniqueId(downloadInfo.url, downloadInfo.file)
            createFileIfPossible(file)
        }
    }

    override fun enqueue(requests: List<Request>): List<Download> {
        val downloadInfoList = requests.map {
            val downloadInfo = it.toDownloadInfo()
            downloadInfo.namespace = namespace
            downloadInfo.status = Status.QUEUED
            prepareDownloadInfoForEnqueue(downloadInfo)
            downloadInfo
        }
        val results = databaseManager.insert(downloadInfoList)
                .filter { it.second }
                .map {
                    logger.d("Enqueued download ${it.first}")
                    it.first
                }
        startPriorityQueueIfNotStarted()
        return results
    }

    override fun enqueueCompletedDownload(completedDownload: CompletedDownload): Download {
        val downloadInfo = completedDownload.toDownloadInfo()
        downloadInfo.namespace = namespace
        downloadInfo.status = Status.COMPLETED
        prepareCompletedDownloadInfoForEnqueue(downloadInfo)
        databaseManager.insert(downloadInfo)
        startPriorityQueueIfNotStarted()
        return downloadInfo
    }

    override fun enqueueCompletedDownloads(completedDownloads: List<CompletedDownload>): List<Download> {
        val downloadInfoList = completedDownloads.map {
            val downloadInfo = it.toDownloadInfo()
            downloadInfo.namespace = namespace
            downloadInfo.status = Status.COMPLETED
            prepareCompletedDownloadInfoForEnqueue(downloadInfo)
            downloadInfo
        }
        val results = databaseManager.insert(downloadInfoList)
                .filter { it.second }
                .map {
                    logger.d("Enqueued CompletedDownload ${it.first}")
                    it.first
                }
        startPriorityQueueIfNotStarted()
        return results
    }

    private fun prepareCompletedDownloadInfoForEnqueue(downloadInfo: DownloadInfo) {
        val existingDownload = databaseManager.getByFile(downloadInfo.file)
        if (existingDownload != null) {
            if (isDownloading(existingDownload.id)) {
                downloadManager.cancel(downloadInfo.id)
            }
            deleteRequestTempFiles(fileTempDir, httpDownloader, existingDownload)
            databaseManager.delete(existingDownload)
        }
    }

    override fun pause(ids: IntArray): List<Download> {
        startPriorityQueueIfNotStarted()
        ids.forEach {
            if (isDownloading(it)) {
                cancelDownload(it)
            }
        }
        val downloadsInfoList = databaseManager.get(ids.toList()).filterNotNull()
        downloadsInfoList.forEach {
            if (canPauseDownload(it)) {
                it.status = Status.PAUSED
            }
        }
        return try {
            databaseManager.update(downloadsInfoList)
            downloadsInfoList
        } catch (e: Exception) {
            logger.e(FETCH_DATABASE_ERROR, e)
            listOf()
        }
    }

    override fun pausedGroup(id: Int): List<Download> {
        startPriorityQueueIfNotStarted()
        var downloadInfoList = databaseManager.getByGroup(id)
        downloadInfoList.forEach {
            if (isDownloading(it.id)) {
                cancelDownload(it.id)
            }
        }
        downloadInfoList = databaseManager.getByGroup(id)
        downloadInfoList.forEach {
            if (canPauseDownload(it)) {
                it.status = Status.PAUSED
            }
        }
        return try {
            databaseManager.update(downloadInfoList)
            downloadInfoList
        } catch (e: Exception) {
            logger.e(FETCH_DATABASE_ERROR, e)
            listOf()
        }
    }

    override fun freeze() {
        startPriorityQueueIfNotStarted()
        priorityListProcessor.pause()
        downloadManager.cancelAll()
        databaseManager.sanitize(true)
    }

    override fun unfreeze() {
        startPriorityQueueIfNotStarted()
        databaseManager.sanitize(true)
        priorityListProcessor.resume()
    }

    override fun resume(ids: IntArray): List<Download> {
        startPriorityQueueIfNotStarted()
        ids.forEach {
            if (isDownloading(it)) {
                cancelDownload(it)
            }
        }
        val downloadsInfoList = databaseManager.get(ids.toList()).filterNotNull()
        downloadsInfoList.forEach {
            if (!isDownloading(it.id) && canResumeDownload(it)) {
                it.status = Status.QUEUED
            }
        }
        return try {
            databaseManager.update(downloadsInfoList)
            downloadsInfoList
        } catch (e: Exception) {
            logger.e(FETCH_DATABASE_ERROR, e)
            listOf()
        }
    }

    override fun resumeGroup(id: Int): List<Download> {
        startPriorityQueueIfNotStarted()
        val downloadsInfoList = databaseManager.getByGroup(id)
                .filter {
                    !isDownloading(it.id) && canResumeDownload(it)
                }
        downloadsInfoList.forEach {
            it.status = Status.QUEUED
        }
        return try {
            databaseManager.update(downloadsInfoList)
            downloadsInfoList
        } catch (e: Exception) {
            logger.e(FETCH_DATABASE_ERROR, e)
            listOf()
        }
    }

    override fun remove(ids: IntArray): List<Download> {
        startPriorityQueueIfNotStarted()
        ids.forEach {
            if (isDownloading(it)) {
                cancelDownload(it)
            }
        }
        val downloadsList = databaseManager.get(ids.toList()).filterNotNull()
        databaseManager.delete(downloadsList)
        val removedStatus = Status.REMOVED
        downloadsList.forEach {
            it.status = removedStatus
            deleteRequestTempFiles(fileTempDir, httpDownloader, it)
        }
        return downloadsList
    }

    override fun removeGroup(id: Int): List<Download> {
        startPriorityQueueIfNotStarted()
        val downloadInfoList = databaseManager.getByGroup(id)
        downloadInfoList.forEach {
            if (isDownloading(it.id)) {
                cancelDownload(it.id)
            }
        }
        databaseManager.delete(downloadInfoList)
        val removedStatus = Status.REMOVED
        downloadInfoList.forEach {
            it.status = removedStatus
            deleteRequestTempFiles(fileTempDir, httpDownloader, it)
        }
        return downloadInfoList
    }

    override fun removeAll(): List<Download> {
        startPriorityQueueIfNotStarted()
        downloadManager.cancelAll()
        val downloadInfoList = databaseManager.get()
        databaseManager.deleteAll()
        val removedStatus = Status.REMOVED
        downloadInfoList.forEach {
            it.status = removedStatus
            deleteRequestTempFiles(fileTempDir, httpDownloader, it)
        }
        return downloadInfoList
    }

    override fun removeAllWithStatus(status: Status): List<Download> {
        startPriorityQueueIfNotStarted()
        val downloadInfoList = databaseManager.getByStatus(status)
        downloadInfoList.forEach {
            if (isDownloading(it.id)) {
                cancelDownload(it.id)
            }
        }
        databaseManager.delete(downloadInfoList)
        val removedStatus = Status.REMOVED
        downloadInfoList.forEach {
            it.status = removedStatus
            deleteRequestTempFiles(fileTempDir, httpDownloader, it)
        }
        return downloadInfoList
    }

    override fun delete(ids: IntArray): List<Download> {
        startPriorityQueueIfNotStarted()
        ids.forEach {
            if (isDownloading(it)) {
                cancelDownload(it)
            }
        }
        val downloadsList = databaseManager.get(ids.toList()).filterNotNull()
        databaseManager.delete(downloadsList)
        val deletedStatus = Status.DELETED
        downloadsList.forEach {
            it.status = deletedStatus
            try {
                val file = File(it.file)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                logger.d("Failed to delete file ${it.file}", e)
            }
            deleteRequestTempFiles(fileTempDir, httpDownloader, it)
        }
        return downloadsList
    }

    override fun deleteGroup(id: Int): List<Download> {
        startPriorityQueueIfNotStarted()
        val downloadInfoList = databaseManager.getByGroup(id)
        downloadInfoList.forEach {
            if (isDownloading(it.id)) {
                cancelDownload(it.id)
            }
        }
        databaseManager.delete(downloadInfoList)
        val deletedStatus = Status.DELETED
        downloadInfoList.forEach {
            it.status = deletedStatus
            try {
                val file = File(it.file)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                logger.d("Failed to delete file ${it.file}", e)
            }
            deleteRequestTempFiles(fileTempDir, httpDownloader, it)
        }
        return downloadInfoList
    }

    override fun deleteAll(): List<Download> {
        startPriorityQueueIfNotStarted()
        downloadManager.cancelAll()
        val downloadInfoList = databaseManager.get()
        databaseManager.deleteAll()
        val deletedStatus = Status.DELETED
        downloadInfoList.forEach {
            it.status = deletedStatus
            try {
                val file = File(it.file)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                logger.d("Failed to delete file ${it.file}", e)
            }
            deleteRequestTempFiles(fileTempDir, httpDownloader, it)
        }
        return downloadInfoList
    }

    override fun deleteAllWithStatus(status: Status): List<Download> {
        startPriorityQueueIfNotStarted()
        val downloadInfoList = databaseManager.getByStatus(status)
        downloadInfoList.forEach {
            if (isDownloading(it.id)) {
                cancelDownload(it.id)
            }
        }
        databaseManager.delete(downloadInfoList)
        val deletedStatus = Status.DELETED
        downloadInfoList.forEach {
            it.status = deletedStatus
            try {
                val file = File(it.file)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                logger.d("Failed to delete file ${it.file}", e)
            }
            deleteRequestTempFiles(fileTempDir, httpDownloader, it)
        }
        return downloadInfoList
    }

    override fun cancel(ids: IntArray): List<Download> {
        startPriorityQueueIfNotStarted()
        ids.forEach {
            if (isDownloading(it)) {
                cancelDownload(it)
            }
        }
        val downloadInfoList = databaseManager.get(ids.toList()).filterNotNull()
        downloadInfoList.forEach {
            if (canCancelDownload(it)) {
                it.status = Status.CANCELLED
                it.error = defaultNoError
            }
        }
        return try {
            databaseManager.update(downloadInfoList)
            downloadInfoList
        } catch (e: Exception) {
            logger.e(FETCH_DATABASE_ERROR, e)
            listOf()
        }
    }

    override fun cancelGroup(id: Int): List<Download> {
        startPriorityQueueIfNotStarted()
        var downloadInfoList = databaseManager.getByGroup(id)
        downloadInfoList.forEach {
            if (isDownloading(it.id)) {
                downloadManager.cancel(it.id)
            }
        }
        downloadInfoList = databaseManager.getByGroup(id)
        downloadInfoList.forEach {
            if (canCancelDownload(it)) {
                it.status = Status.CANCELLED
                it.error = defaultNoError
            }
        }
        return try {
            databaseManager.update(downloadInfoList)
            downloadInfoList
        } catch (e: Exception) {
            logger.e(FETCH_DATABASE_ERROR, e)
            listOf()
        }
    }

    override fun cancelAll(): List<Download> {
        startPriorityQueueIfNotStarted()
        var downloadInfoList = databaseManager.get()
        downloadInfoList.forEach {
            if (isDownloading(it.id)) {
                downloadManager.cancel(it.id)
            }
        }
        downloadInfoList = databaseManager.get()
        downloadInfoList.forEach {
            if (canCancelDownload(it)) {
                it.status = Status.CANCELLED
                it.error = defaultNoError
            }
        }
        return try {
            databaseManager.update(downloadInfoList)
            downloadInfoList
        } catch (e: Exception) {
            logger.e(FETCH_DATABASE_ERROR, e)
            listOf()
        }
    }

    override fun retry(ids: IntArray): List<Download> {
        startPriorityQueueIfNotStarted()
        val downloadInfoList = databaseManager.get(ids.toList()).filterNotNull()
        downloadInfoList.forEach {
            if (canRetryDownload(it)) {
                it.status = Status.QUEUED
                it.error = defaultNoError
            }
        }
        return try {
            databaseManager.update(downloadInfoList)
            downloadInfoList
        } catch (e: Exception) {
            logger.e(FETCH_DATABASE_ERROR, e)
            listOf()
        }
    }

    override fun updateRequest(id: Int, requestInfo: RequestInfo): Download? {
        startPriorityQueueIfNotStarted()
        var downloadInfo = databaseManager.get(id)
        if (downloadInfo != null) {
            if (isDownloading(id)) {
                cancelDownload(id)
            }
            downloadInfo = databaseManager.get(id)
            if (downloadInfo != null) {
                downloadInfo.group = requestInfo.groupId
                downloadInfo.headers = requestInfo.headers
                downloadInfo.priority = requestInfo.priority
                downloadInfo.networkType = requestInfo.networkType
                downloadInfo.enqueueAction = requestInfo.enqueueAction
                databaseManager.update(downloadInfo)
                return downloadInfo
            }
        }
        return null
    }

    override fun getDownloads(): List<Download> {
        startPriorityQueueIfNotStarted()
        return databaseManager.get()
    }

    override fun getDownload(id: Int): Download? {
        startPriorityQueueIfNotStarted()
        return databaseManager.get(id)
    }

    override fun getDownloads(idList: List<Int>): List<Download> {
        startPriorityQueueIfNotStarted()
        val downloads = databaseManager.get(idList)
        val results = mutableListOf<Download>()
        downloads.filterNotNull().forEach {
            results.add(it)
        }
        return results
    }

    override fun getDownloadsInGroup(id: Int): List<Download> {
        startPriorityQueueIfNotStarted()
        return databaseManager.getByGroup(id)
    }

    override fun getDownloadsWithStatus(status: Status): List<Download> {
        startPriorityQueueIfNotStarted()
        return databaseManager.getByStatus(status)
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): List<Download> {
        startPriorityQueueIfNotStarted()
        return databaseManager.getDownloadsInGroupWithStatus(groupId, status)
    }

    override fun getDownloadsByRequestIdentifier(identifier: Long): List<Download> {
        startPriorityQueueIfNotStarted()
        return databaseManager.getDownloadsByRequestIdentifier(identifier)
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
        startPriorityQueueIfNotStarted()
        priorityListProcessor.globalNetworkType = networkType
        downloadManager.cancelAll()
        databaseManager.sanitize(true)
    }

    override fun enableLogging(enabled: Boolean) {
        startPriorityQueueIfNotStarted()
        logger.d("Enable logging - $enabled")
        logger.enabled = enabled
    }

    override fun addListener(listener: FetchListener, notify: Boolean) {
        startPriorityQueueIfNotStarted()
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
                            listener.onError(it)
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
                        Status.NONE -> {
                        }
                    }
                }
            }
        }
        logger.d("Added listener $listener")
    }

    override fun removeListener(listener: FetchListener) {
        startPriorityQueueIfNotStarted()
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

    override fun isDownloading(id: Int): Boolean {
        startPriorityQueueIfNotStarted()
        return downloadManager.contains(id)
    }

    override fun cancelDownload(id: Int): Boolean {
        startPriorityQueueIfNotStarted()
        return downloadManager.cancel(id)
    }

    private fun startPriorityQueueIfNotStarted() {
        if (priorityListProcessor.isStopped && !isTerminating) {
            priorityListProcessor.start()
        }
        if (priorityListProcessor.isPaused && !isTerminating) {
            priorityListProcessor.resume()
        }
    }

}