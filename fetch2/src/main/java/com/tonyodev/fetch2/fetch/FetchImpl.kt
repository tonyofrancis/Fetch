package com.tonyodev.fetch2.fetch


import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.database.FetchDatabaseManagerWrapper
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.getErrorFromMessage
import com.tonyodev.fetch2.fetch.FetchModulesBuilder.Modules
import com.tonyodev.fetch2.util.ActiveDownloadInfo
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_AUTOSTART_ON_ATTACHED
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED
import com.tonyodev.fetch2.util.toDownloadInfo
import com.tonyodev.fetch2core.*

open class FetchImpl constructor(override val namespace: String,
                                 final override val fetchConfiguration: FetchConfiguration,
                                 private val handlerWrapper: HandlerWrapper,
                                 private val uiHandler: Handler,
                                 private val fetchHandler: FetchHandler,
                                 private val logger: Logger,
                                 private val listenerCoordinator: ListenerCoordinator,
                                 private val fetchDatabaseManagerWrapper: FetchDatabaseManagerWrapper) : Fetch {

    private val lock = Object()
    @Volatile
    private var closed = false
    override val isClosed: Boolean
        get() {
            synchronized(lock) {
                return closed
            }
        }
    private val activeDownloadsSet = mutableSetOf<ActiveDownloadInfo>()
    private val activeDownloadsRunnable = Runnable {
        if (!isClosed) {
            val hasActiveDownloadsAdded = fetchHandler.hasActiveDownloads(true)
            val hasActiveDownloads = fetchHandler.hasActiveDownloads(false)
            uiHandler.post {
                if (!isClosed) {
                    val iterator = activeDownloadsSet.iterator()
                    var activeDownloadInfo: ActiveDownloadInfo
                    var hasActive: Boolean
                    while (iterator.hasNext()) {
                        activeDownloadInfo = iterator.next()
                        hasActive = if (activeDownloadInfo.includeAddedDownloads) hasActiveDownloadsAdded else hasActiveDownloads
                        activeDownloadInfo.fetchObserver.onChanged(hasActive, Reason.REPORTING)
                    }
                }
                if (!isClosed) {
                    registerActiveDownloadsRunnable()
                }
            }
        }
    }

    init {
        handlerWrapper.post {
            fetchHandler.init()
        }
        registerActiveDownloadsRunnable()
    }

    private fun registerActiveDownloadsRunnable() {
        handlerWrapper.postDelayed(activeDownloadsRunnable, fetchConfiguration.activeDownloadsCheckInterval)
    }

    override fun enqueue(request: Request, func: Func<Request>?, func2: Func<Error>?): Fetch {
        enqueueRequest(listOf(request), Func { result ->
            if (result.isNotEmpty()) {
                val enqueuedPair = result.first()
                if (enqueuedPair.second != Error.NONE) {
                    uiHandler.post {
                        func2?.call(enqueuedPair.second)
                    }
                } else {
                    uiHandler.post {
                        func?.call(enqueuedPair.first)
                    }
                }
            } else {
                uiHandler.post {
                    func2?.call(Error.ENQUEUE_NOT_SUCCESSFUL)
                }
            }
        }, func2)
        return this
    }

    override fun enqueue(requests: List<Request>, func: Func<List<Pair<Request, Error>>>?): Fetch {
        enqueueRequest(requests, func, null)
        return this
    }

    private fun enqueueRequest(requests: List<Request>, func: Func<List<Pair<Request, Error>>>?, func2: Func<Error>?) {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val distinctCount = requests.distinctBy { it.file }.count()
                    if (distinctCount != requests.size) {
                        throw FetchException(ENQUEUED_REQUESTS_ARE_NOT_DISTINCT)
                    }
                    val downloadPairs = fetchHandler.enqueue(requests)
                    downloadPairs.forEach { downloadPair ->
                        val download = downloadPair.first
                        when (download.status) {
                            Status.ADDED -> {
                                listenerCoordinator.mainListener.onAdded(download)
                                logger.d("Added $download")
                            }
                            Status.QUEUED -> {
                                val downloadCopy = download.toDownloadInfo(fetchDatabaseManagerWrapper.getNewDownloadInfoInstance())
                                downloadCopy.status = Status.ADDED
                                listenerCoordinator.mainListener.onAdded(downloadCopy)
                                logger.d("Added $download")
                                listenerCoordinator.mainListener.onQueued(download, false)
                                logger.d("Queued $download for download")
                            }
                            Status.COMPLETED -> {
                                listenerCoordinator.mainListener.onCompleted(download)
                                logger.d("Completed download $download")
                            }
                            else -> {

                            }
                        }
                    }
                    uiHandler.post {
                        func?.call(downloadPairs.map { Pair(it.first.request, it.second) })
                    }
                } catch (e: Exception) {
                    logger.e("Failed to enqueue list $requests")
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
        }
    }

    override fun pause(ids: List<Int>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        pauseDownloads(ids, null, func, func2)
        return this
    }

    override fun pause(ids: List<Int>): Fetch {
        return pause(ids, null, null)
    }

    override fun pause(id: Int, func: Func<Download>?, func2: Func<Error>?): Fetch {
        return pause(listOf(id), Func { downloads ->
            if (downloads.isNotEmpty()) {
                func?.call(downloads.first())
            } else {
                func2?.call(Error.REQUEST_DOES_NOT_EXIST)
            }
        }, func2)
    }

    override fun pause(id: Int): Fetch {
        return pause(id, null, null)
    }

    override fun pauseGroup(id: Int, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        pauseDownloads(null, id, func, func2)
        return this
    }

    private fun pauseDownloads(ids: List<Int>?, groupId: Int?, func: Func<List<Download>>?, func2: Func<Error>?) {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = if (ids != null) {
                        fetchHandler.pause(ids)
                    } else {
                        if (groupId != null) {
                            fetchHandler.pausedGroup(groupId)
                        } else {
                            listOf()
                        }
                    }
                    downloads.forEach {
                        logger.d("Paused download $it")
                        listenerCoordinator.mainListener.onPaused(it)
                    }
                    uiHandler.post {
                        func?.call(downloads)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
        }
    }

    override fun pauseGroup(id: Int): Fetch {
        return pauseGroup(id, null, null)
    }

    override fun pauseAll(): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.pauseAll()
                    downloads.forEach {
                        logger.d("Paused download $it")
                        listenerCoordinator.mainListener.onPaused(it)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                }
            }
        }
        return this
    }

    override fun freeze(func: Func<Boolean>?, func2: Func<Error>?): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    fetchHandler.freeze()
                    if (func != null) {
                        uiHandler.post {
                            func.call(true)
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            this
        }
    }

    override fun freeze(): Fetch {
        return freeze(null, null)
    }

    override fun unfreeze(func: Func<Boolean>?, func2: Func<Error>?): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    fetchHandler.unfreeze()
                    if (func != null) {
                        uiHandler.post {
                            func.call(true)
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            this
        }
    }

    override fun unfreeze(): Fetch {
        return unfreeze(null, null)
    }

    override fun resume(ids: List<Int>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        resumeDownloads(ids, null, func, func2)
        return this
    }

    override fun resume(ids: List<Int>): Fetch {
        return resume(ids, null, null)
    }

    override fun resume(id: Int, func: Func<Download>?, func2: Func<Error>?): Fetch {
        return resume(listOf(id), Func { downloads ->
            if (downloads.isNotEmpty()) {
                func?.call(downloads.first())
            } else {
                func2?.call(Error.REQUEST_DOES_NOT_EXIST)
            }
        }, func2)
    }

    override fun resume(id: Int): Fetch {
        return resume(id, null, null)
    }

    override fun resumeGroup(id: Int, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        resumeDownloads(null, id, func, func2)
        return this
    }

    override fun resumeGroup(id: Int): Fetch {
        return resumeGroup(id, null, null)
    }

    private fun resumeDownloads(ids: List<Int>?, groupId: Int?, func: Func<List<Download>>?, func2: Func<Error>?) {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = if (ids != null) {
                        fetchHandler.resume(ids)
                    } else {
                        if (groupId != null) {
                            fetchHandler.resumeGroup(groupId)
                        } else {
                            listOf()
                        }
                    }
                    downloads.forEach {
                        logger.d("Queued download $it")
                        listenerCoordinator.mainListener.onQueued(it, false)
                        logger.d("Resumed download $it")
                        listenerCoordinator.mainListener.onResumed(it)
                    }
                    uiHandler.post {
                        func?.call(downloads)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
        }
    }

    override fun resumeAll(): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.resumeAll()
                    downloads.forEach {
                        logger.d("Queued download $it")
                        listenerCoordinator.mainListener.onQueued(it, false)
                        logger.d("Resumed download $it")
                        listenerCoordinator.mainListener.onResumed(it)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                }
            }
        }
        return this
    }

    override fun remove(ids: List<Int>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeRemoveAction({ fetchHandler.remove(ids) }, func, func2)
    }

    override fun remove(ids: List<Int>): Fetch {
        return remove(ids, null, null)
    }

    override fun remove(id: Int): Fetch {
        return remove(id, null, null)
    }

    override fun remove(id: Int, func: Func<Download>?, func2: Func<Error>?): Fetch {
        return remove(listOf(id), Func { downloads ->
            if (downloads.isNotEmpty()) {
                func?.call(downloads.first())
            } else {
                func2?.call(Error.REQUEST_DOES_NOT_EXIST)
            }
        }, func2)
    }

    override fun removeGroup(id: Int, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeRemoveAction({ fetchHandler.removeGroup(id) }, func, func2)
    }

    override fun removeGroup(id: Int): Fetch {
        return removeGroup(id, null, null)
    }

    override fun removeAll(func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeRemoveAction({ fetchHandler.removeAll() }, func, func2)
    }

    override fun removeAll(): Fetch {
        return removeAll(null, null)
    }

    override fun removeAllWithStatus(status: Status, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeRemoveAction({ fetchHandler.removeAllWithStatus(status) }, func, func2)
    }

    override fun removeAllWithStatus(status: Status): Fetch {
        return removeAllWithStatus(status, null, null)
    }

    override fun removeAllInGroupWithStatus(id: Int, statuses: List<Status>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeRemoveAction({ fetchHandler.removeAllInGroupWithStatus(id, statuses) }, func, func2)
    }

    override fun removeAllInGroupWithStatus(id: Int, statuses: List<Status>): Fetch {
        return removeAllInGroupWithStatus(id, statuses, null, null)
    }

    private fun executeRemoveAction(downloadAction: () -> List<Download>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = downloadAction.invoke()
                    downloads.forEach {
                        logger.d("Removed download $it")
                        listenerCoordinator.mainListener.onRemoved(it)
                    }
                    uiHandler.post {
                        func?.call(downloads)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            this
        }
    }

    override fun delete(ids: List<Int>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeDeleteAction({ fetchHandler.delete(ids) }, func, func2)
    }

    override fun delete(id: Int, func: Func<Download>?, func2: Func<Error>?): Fetch {
        return delete(listOf(id), Func { downloads ->
            if (downloads.isNotEmpty()) {
                func?.call(downloads.first())
            } else {
                func2?.call(Error.REQUEST_DOES_NOT_EXIST)
            }
        }, func2)
    }

    override fun deleteGroup(id: Int, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeDeleteAction({ fetchHandler.deleteGroup(id) }, func, func2)
    }

    override fun delete(ids: List<Int>): Fetch {
        return delete(ids, null, null)
    }

    override fun delete(id: Int): Fetch {
        return delete(id, null, null)
    }

    override fun deleteGroup(id: Int): Fetch {
        return deleteGroup(id, null, null)
    }

    override fun deleteAll(func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeDeleteAction({ fetchHandler.deleteAll() }, func, func2)
    }

    override fun deleteAll(): Fetch {
        return deleteAll(null, null)
    }

    override fun deleteAllWithStatus(status: Status, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeDeleteAction({ fetchHandler.deleteAllWithStatus(status) }, func, func2)
    }

    override fun deleteAllWithStatus(status: Status): Fetch {
        return deleteAllWithStatus(status, null, null)
    }

    override fun deleteAllInGroupWithStatus(id: Int, statuses: List<Status>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeDeleteAction({ fetchHandler.deleteAllInGroupWithStatus(id, statuses) }, func, func2)
    }

    override fun deleteAllInGroupWithStatus(id: Int, statuses: List<Status>): Fetch {
        return deleteAllInGroupWithStatus(id, statuses, null, null)
    }

    private fun executeDeleteAction(downloadAction: () -> List<Download>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = downloadAction.invoke()
                    downloads.forEach {
                        logger.d("Deleted download $it")
                        listenerCoordinator.mainListener.onDeleted(it)
                    }
                    uiHandler.post {
                        func?.call(downloads)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            this
        }
    }

    override fun cancel(ids: List<Int>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeCancelAction({ fetchHandler.cancel(ids) }, func, func2)
    }

    override fun cancel(ids: List<Int>): Fetch {
        return cancel(ids, null, null)
    }

    override fun cancel(id: Int, func: Func<Download>?, func2: Func<Error>?): Fetch {
        return cancel(listOf(id), Func { downloads ->
            if (downloads.isNotEmpty()) {
                func?.call(downloads.first())
            } else {
                func2?.call(Error.REQUEST_DOES_NOT_EXIST)
            }
        }, func2)
    }

    override fun cancel(id: Int): Fetch {
        return cancel(id, null, null)
    }

    override fun cancelGroup(id: Int, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeCancelAction({ fetchHandler.cancelGroup(id) }, func, func2)
    }

    override fun cancelGroup(id: Int): Fetch {
        return cancelGroup(id, null, null)
    }

    override fun cancelAll(func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return executeCancelAction({ fetchHandler.cancelAll() }, func, func2)
    }

    override fun cancelAll(): Fetch {
        return cancelAll(null, null)
    }

    private fun executeCancelAction(downloadAction: () -> List<Download>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = downloadAction.invoke()
                    downloads.forEach {
                        logger.d("Cancelled download $it")
                        listenerCoordinator.mainListener.onCancelled(it)
                    }
                    uiHandler.post {
                        func?.call(downloads)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            this
        }
    }

    override fun retry(ids: List<Int>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.retry(ids)
                    downloads.forEach {
                        logger.d("Queued $it for download")
                        listenerCoordinator.mainListener.onQueued(it, false)
                    }
                    uiHandler.post {
                        func?.call(downloads)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            this
        }
    }

    override fun resetAutoRetryAttempts(downloadId: Int, retryDownload: Boolean, func: Func2<Download?>?, func2: Func<Error>?): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val download = fetchHandler.resetAutoRetryAttempts(downloadId, retryDownload)
                    if (download != null && download.status == Status.QUEUED) {
                        logger.d("Queued $download for download")
                        listenerCoordinator.mainListener.onQueued(download, false)
                    }
                    uiHandler.post {
                        func?.call(download)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            this
        }
    }

    override fun retry(ids: List<Int>): Fetch {
        return retry(ids, null, null)
    }

    override fun retry(id: Int, func: Func<Download>?, func2: Func<Error>?): Fetch {
        return retry(listOf(id), Func { downloads ->
            if (downloads.isNotEmpty()) {
                func?.call(downloads.first())
            } else {
                func2?.call(Error.REQUEST_DOES_NOT_EXIST)
            }
        }, func2)
    }

    override fun retry(id: Int): Fetch {
        return retry(id, null, null)
    }

    override fun updateRequest(requestId: Int, updatedRequest: Request, notifyListeners: Boolean, func: Func<Download>?,
                               func2: Func<Error>?): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloadPair = fetchHandler.updateRequest(requestId, updatedRequest)
                    val download = downloadPair.first
                    logger.d("UpdatedRequest with id: $requestId to $download")
                    if (notifyListeners) {
                        when (download.status) {
                            Status.COMPLETED -> {
                                listenerCoordinator.mainListener.onCompleted(download)
                            }
                            Status.FAILED -> {
                                listenerCoordinator.mainListener.onError(download, download.error, null)
                            }
                            Status.CANCELLED -> {
                                listenerCoordinator.mainListener.onCancelled(download)
                            }
                            Status.DELETED -> {
                                listenerCoordinator.mainListener.onDeleted(download)
                            }
                            Status.PAUSED -> {
                                listenerCoordinator.mainListener.onPaused(download)
                            }
                            Status.QUEUED -> {
                                if (!downloadPair.second) {
                                    val downloadCopy = download.toDownloadInfo(fetchDatabaseManagerWrapper.getNewDownloadInfoInstance())
                                    downloadCopy.status = Status.ADDED
                                    listenerCoordinator.mainListener.onAdded(downloadCopy)
                                    logger.d("Added $download")
                                }
                                listenerCoordinator.mainListener.onQueued(download, false)
                            }
                            Status.REMOVED -> {
                                listenerCoordinator.mainListener.onRemoved(download)
                            }
                            Status.DOWNLOADING -> {
                            }
                            Status.ADDED -> {
                                listenerCoordinator.mainListener.onAdded(download)
                            }
                            Status.NONE -> {
                            }
                        }
                    }
                    uiHandler.post {
                        func?.call(download)
                    }
                } catch (e: Exception) {
                    logger.e("Failed to update request with id $requestId", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            this
        }
    }

    override fun renameCompletedDownloadFile(id: Int, newFileName: String, func: Func<Download>?, func2: Func<Error>?): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val download = fetchHandler.renameCompletedDownloadFile(id, newFileName)
                    if (func != null) {
                        uiHandler.post {
                            func.call(download)
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Failed to rename file on download with id $id", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            this
        }
    }

    override fun replaceExtras(id: Int, extras: Extras, func: Func<Download>?, func2: Func<Error>?): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val download = fetchHandler.replaceExtras(id, extras)
                    if (func != null) {
                        uiHandler.post {
                            func.call(download)
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Failed to replace extras on download with id $id", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            this
        }
    }

    override fun getDownloads(func: Func<List<Download>>): Fetch {
        return synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val downloads = fetchHandler.getDownloads()
                uiHandler.post {
                    func.call(downloads)
                }
            }
            this
        }
    }

    override fun getDownload(id: Int, func2: Func2<Download?>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val download = fetchHandler.getDownload(id)
                uiHandler.post {
                    func2.call(download)
                }
            }
            return this
        }
    }

    override fun getDownloads(idList: List<Int>, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val downloads = fetchHandler.getDownloads(idList)
                uiHandler.post {
                    func.call(downloads)
                }
            }
            return this
        }
    }

    override fun getDownloadsInGroup(groupId: Int, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val downloads = fetchHandler.getDownloadsInGroup(groupId)
                uiHandler.post {
                    func.call(downloads)
                }
            }
            return this
        }
    }

    override fun getDownloadsWithStatus(status: Status, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val downloads = fetchHandler.getDownloadsWithStatus(status)
                uiHandler.post {
                    func.call(downloads)
                }
            }
            return this
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, statuses: List<Status>, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val downloads = fetchHandler.getDownloadsInGroupWithStatus(groupId, statuses)
                uiHandler.post {
                    func.call(downloads)
                }
            }
            return this
        }
    }

    override fun getDownloadsByRequestIdentifier(identifier: Long, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val downloads = fetchHandler.getDownloadsByRequestIdentifier(identifier)
                uiHandler.post {
                    func.call(downloads)
                }
            }
            return this
        }
    }

    override fun getDownloadsWithStatus(statuses: List<Status>, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val downloads = fetchHandler.getDownloadsWithStatus(statuses)
                uiHandler.post {
                    func.call(downloads)
                }
            }
            return this
        }
    }

    override fun getAllGroupIds(func: Func<List<Int>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val groupIdList = fetchHandler.getAllGroupIds()
                uiHandler.post {
                    func.call(groupIdList)
                }
            }
            return this
        }
    }

    override fun getDownloadsByTag(tag: String, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val downloads = fetchHandler.getDownloadsByTag(tag)
                uiHandler.post {
                    func.call(downloads)
                }
            }
            return this
        }
    }

    override fun addCompletedDownload(completedDownload: CompletedDownload, alertListeners: Boolean, func: Func<Download>?, func2: Func<Error>?): Fetch {
        return addCompletedDownloads(listOf(completedDownload), alertListeners, Func { downloads ->
            if (downloads.isNotEmpty()) {
                func?.call(downloads.first())
            } else {
                func2?.call(Error.COMPLETED_NOT_ADDED_SUCCESSFULLY)
            }
        }, func2)
    }

    override fun addCompletedDownloads(completedDownloads: List<CompletedDownload>, alertListeners: Boolean, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.enqueueCompletedDownloads(completedDownloads)
                    if (alertListeners) {
                        downloads.forEach {
                            listenerCoordinator.mainListener.onCompleted(it)
                            logger.d("Added CompletedDownload $it")
                        }
                    }
                    uiHandler.post {
                        func?.call(downloads)
                    }
                } catch (e: Exception) {
                    logger.e("Failed to add CompletedDownload list $completedDownloads")
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }

            }
            return this
        }
    }

    override fun getFetchGroup(group: Int, func: Func<FetchGroup>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val fetchGroup = fetchHandler.getFetchGroup(group)
                uiHandler.post {
                    func.call(fetchGroup)
                }
            }
        }
        return this
    }

    override fun hasActiveDownloads(includeAddedDownloads: Boolean, func: Func<Boolean>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val hasActiveDownloads = fetchHandler.hasActiveDownloads(includeAddedDownloads)
                uiHandler.post {
                    func.call(hasActiveDownloads)
                }
            }
        }
        return this
    }

    override fun addListener(listener: FetchListener): Fetch {
        return addListener(listener, DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED)
    }

    override fun addListener(listener: FetchListener, notify: Boolean): Fetch {
        return addListener(listener, notify, DEFAULT_ENABLE_LISTENER_AUTOSTART_ON_ATTACHED)
    }

    override fun addListener(listener: FetchListener, notify: Boolean, autoStart: Boolean): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.addListener(listener, notify, autoStart)
            }
            return this
        }
    }

    override fun removeListener(listener: FetchListener): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.removeListener(listener)
            }
            return this
        }
    }

    override fun getListenerSet(): Set<FetchListener> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            fetchHandler.getListenerSet()
        }
    }

    override fun attachFetchObserversForDownload(downloadId: Int, vararg fetchObservers: FetchObserver<Download>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.addFetchObserversForDownload(downloadId, *fetchObservers)
            }
            return this
        }
    }

    override fun removeFetchObserversForDownload(downloadId: Int, vararg fetchObservers: FetchObserver<Download>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.removeFetchObserversForDownload(downloadId, *fetchObservers)
            }
            return this
        }
    }

    override fun getDownloadBlocks(downloadId: Int, func: Func<List<DownloadBlock>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val downloadBlocksList = fetchHandler.getDownloadBlocks(downloadId)
                uiHandler.post {
                    func.call(downloadBlocksList)
                }
            }
            return this
        }
    }

    override fun getContentLengthForRequest(request: Request, fromServer: Boolean, func: Func<Long>, func2: Func<Error>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.executeWorkerTask {
                try {
                    val contentLength = fetchHandler.getContentLengthForRequest(request, fromServer)
                    uiHandler.post {
                        func.call(contentLength)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            return this
        }
    }

    override fun getContentLengthForRequests(requests: List<Request>, fromServer: Boolean, func: Func<List<Pair<Request, Long>>>, func2: Func<List<Pair<Request, Error>>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.executeWorkerTask {
                val results = mutableListOf<Pair<Request, Long>>()
                val results2 = mutableListOf<Pair<Request, Error>>()
                for (request in requests) {
                    try {
                        results.add(Pair(request, fetchHandler.getContentLengthForRequest(request, fromServer)))
                    } catch (e: Exception) {
                        logger.e("Fetch with namespace $namespace error", e)
                        val error = getErrorFromMessage(e.message)
                        error.throwable = e
                        results2.add(Pair(request, error))
                    }
                }
                uiHandler.post {
                    func.call(results)
                }
                uiHandler.post {
                    func2.call(results2)
                }
            }
            return this
        }
    }


    override fun getServerResponse(url: String,
                                   headers: Map<String, String>?,
                                   func: Func<Downloader.Response>,
                                   func2: Func<Error>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.executeWorkerTask {
                try {
                    val response = fetchHandler.getServerResponse(url, headers)
                    uiHandler.post {
                        func.call(response)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            return this
        }
    }

    override fun getFetchFileServerCatalog(request: Request, func: Func<List<FileResource>>, func2: Func<Error>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.executeWorkerTask {
                try {
                    val fileResourceList = fetchHandler.getFetchFileServerCatalog(request)
                    uiHandler.post {
                        func.call(fileResourceList)
                    }
                } catch (e: Exception) {
                    logger.e("Fetch with namespace $namespace error", e)
                    val error = getErrorFromMessage(e.message)
                    error.throwable = e
                    if (func2 != null) {
                        uiHandler.post {
                            func2.call(error)
                        }
                    }
                }
            }
            return this
        }
    }

    override fun setGlobalNetworkType(networkType: NetworkType): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.setGlobalNetworkType(networkType)
            }
            return this
        }
    }

    override fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            if (downloadConcurrentLimit < 0) {
                throw FetchException("Concurrent limit cannot be less than 0")
            }
            handlerWrapper.post {
                fetchHandler.setDownloadConcurrentLimit(downloadConcurrentLimit)
            }
            return this
        }
    }

    override fun enableLogging(enabled: Boolean): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.enableLogging(enabled)
            }
            return this
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
            logger.d("$namespace closing/shutting down")
            handlerWrapper.removeCallbacks(activeDownloadsRunnable)
            handlerWrapper.post {
                try {
                    fetchHandler.close()
                } catch (e: Exception) {
                    logger.e("exception occurred whiles shutting down Fetch with namespace:$namespace", e)
                }
            }
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchException("This fetch instance has been closed. Create a new " +
                    "instance using the builder.")
        }
    }

    override fun awaitFinishOrTimeout(allowTimeInMilliseconds: Long) {
        com.tonyodev.fetch2.util.awaitFinishOrTimeout(allowTimeInMilliseconds, fetchHandler)
    }

    override fun awaitFinish() {
        awaitFinishOrTimeout(-1)
    }

    override fun addActiveDownloadsObserver(includeAddedDownloads: Boolean, fetchObserver: FetchObserver<Boolean>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                activeDownloadsSet.add(ActiveDownloadInfo(fetchObserver, includeAddedDownloads))
            }
            return this
        }
    }

    override fun removeActiveDownloadsObserver(fetchObserver: FetchObserver<Boolean>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                val iterator = activeDownloadsSet.iterator()
                while (iterator.hasNext()) {
                    val activeDownloadInfo = iterator.next()
                    if (activeDownloadInfo.fetchObserver == fetchObserver) {
                        iterator.remove()
                        logger.d("Removed ActiveDownload FetchObserver $fetchObserver")
                        break
                    }
                }
            }
            return this
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(modules: Modules): FetchImpl {
            return FetchImpl(
                    namespace = modules.fetchConfiguration.namespace,
                    fetchConfiguration = modules.fetchConfiguration,
                    handlerWrapper = modules.handlerWrapper,
                    uiHandler = modules.uiHandler,
                    fetchHandler = modules.fetchHandler,
                    logger = modules.fetchConfiguration.logger,
                    listenerCoordinator = modules.listenerCoordinator,
                    fetchDatabaseManagerWrapper = modules.fetchDatabaseManagerWrapper)
        }

    }

}

