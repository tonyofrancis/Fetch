package com.tonyodev.fetch2.fetch

import android.os.Build
import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.database.DatabaseManager
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2.provider.ListenerProvider
import com.tonyodev.fetch2.helper.PriorityListProcessor
import com.tonyodev.fetch2.util.*
import java.io.File

/**
 * This handler class handles all tasks and operations of Fetch.
 * */
class FetchHandlerImpl(private val namespace: String,
                       private val databaseManager: DatabaseManager,
                       private val downloadManager: DownloadManager,
                       private val priorityListProcessor: PriorityListProcessor<Download>,
                       override val fetchListenerProvider: ListenerProvider,
                       private val handler: Handler,
                       private val logger: Logger,
                       private val autoStart: Boolean) : FetchHandler {

    @Volatile
    private var isTerminating = false

    override fun init() {
        databaseManager.verifyDatabase()
        if (autoStart) {
            priorityListProcessor.start()
        }
    }

    override fun enqueue(request: Request): Download {
        startPriorityQueueIfNotStarted()
        val downloadInfo = request.toDownloadInfo()
        downloadInfo.namespace = namespace
        downloadInfo.status = Status.QUEUED
        databaseManager.insert(downloadInfo)
        return downloadInfo
    }

    override fun enqueue(requests: List<Request>): List<Download> {
        startPriorityQueueIfNotStarted()
        val downloadInfoList = requests.map {
            val downloadInfo = it.toDownloadInfo()
            downloadInfo.namespace = namespace
            downloadInfo.status = Status.QUEUED
            downloadInfo
        }
        val results = mutableListOf<Download>()
        val insertedList = databaseManager.insert(downloadInfoList)
        insertedList.forEach {
            if (it.second) {
                logger.d("Enqueued download ${it.first}")
                results.add(it.first)
            }
        }
        return results
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
        downloadManager.cancelAll()
        priorityListProcessor.pause()
        databaseManager.verifyDatabase()
    }

    override fun unfreeze() {
        startPriorityQueueIfNotStarted()
        databaseManager.verifyDatabase()
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

    override fun close() {
        if (isTerminating) {
            return
        }
        isTerminating = true
        fetchListenerProvider.listeners.clear()
        priorityListProcessor.stop()
        downloadManager.terminateAllDownloads()
        handler.post {
            try {
                downloadManager.close()
                databaseManager.close()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    handler.looper.quitSafely()
                } else {
                    handler.looper.quit()
                }
            } catch (e: Exception) {
                logger.e("FetchHandler", e)
            }
        }
        FetchModulesBuilder.removeActiveFetchHandlerNamespaceInstance(namespace)
    }

    override fun setGlobalNetworkType(networkType: NetworkType) {
        startPriorityQueueIfNotStarted()
        priorityListProcessor.globalNetworkType = networkType
        downloadManager.cancelAll()
        databaseManager.verifyDatabase()
    }

    override fun enableLogging(enabled: Boolean) {
        startPriorityQueueIfNotStarted()
        logger.d("Enable logging - $enabled")
        logger.enabled = enabled
    }

    override fun addListener(listener: FetchListener) {
        startPriorityQueueIfNotStarted()
        fetchListenerProvider.listeners.add(listener)
        logger.d("Added listener $listener")
    }

    override fun removeListener(listener: FetchListener) {
        startPriorityQueueIfNotStarted()
        val iterator = fetchListenerProvider.listeners.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == listener) {
                logger.d("Removed listener $listener")
                iterator.remove()
                break
            }
        }
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