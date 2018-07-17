package com.tonyodev.fetch2.fetch


import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.getErrorFromMessage
import com.tonyodev.fetch2.fetch.FetchModulesBuilder.Modules
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED
import com.tonyodev.fetch2core.*

open class FetchImpl constructor(override val namespace: String,
                                 protected val handlerWrapper: HandlerWrapper,
                                 protected val uiHandler: Handler,
                                 val fetchHandler: FetchHandler,
                                 protected val logger: Logger,
                                 protected val listenerCoordinator: ListenerCoordinator) : Fetch {

    protected val lock = Object()
    @Volatile
    private var closed = false
    override val isClosed: Boolean
        get() = closed

    init {
        handlerWrapper.post {
            fetchHandler.init()
        }
    }

    override fun enqueue(request: Request, func: Func<Request>?, func2: Func<Error>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val download = fetchHandler.enqueue(request)
                    if (func != null) {
                        uiHandler.post {
                            func.call(download.request)
                        }
                    }
                    uiHandler.post {
                        listenerCoordinator.mainListener.onAdded(download)
                        logger.d("Added Download $download")
                        if (download.status == Status.QUEUED) {
                            listenerCoordinator.mainListener.onQueued(download, false)
                            logger.d("Queued $download for download")
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Failed to enqueue request $request", e)
                    val error = getErrorFromMessage(e.message)
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

    override fun enqueue(requests: List<Request>, func: Func<List<Request>>?, func2: Func<Error>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.enqueue(requests)
                    if (func != null) {
                        uiHandler.post {
                            func.call(downloads.map { it.request })
                        }
                    }
                    uiHandler.post {
                        downloads.forEach {
                            listenerCoordinator.mainListener.onAdded(it)
                            logger.d("Added $it")
                            if (it.status == Status.QUEUED) {
                                listenerCoordinator.mainListener.onQueued(it, false)
                                logger.d("Queued $it for download")
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Failed to enqueue list $requests")
                    val error = getErrorFromMessage(e.message)
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

    override fun pause(ids: List<Int>, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.pause(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Paused download $it")
                            listenerCoordinator.mainListener.onPaused(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    uiHandler.post {
                        func?.call(emptyList())
                    }
                }
            }
            return this
        }
    }

    override fun pause(ids: List<Int>): Fetch {
        return pause(ids, null)
    }

    override fun pause(id: Int, func2: Func2<Download?>?): Fetch {
        return pause(listOf(id), Func { func2?.call(it.firstOrNull()) })
    }

    override fun pause(id: Int): Fetch {
        return pause(id, null)
    }

    override fun pauseGroup(id: Int, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.pausedGroup(id)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Paused download $it")
                            listenerCoordinator.mainListener.onPaused(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    uiHandler.post {
                        func?.call(emptyList())
                    }
                }
            }
            return this
        }
    }

    override fun pauseGroup(id: Int): Fetch {
        return pauseGroup(id, null)
    }

    override fun freeze(func: Func<Boolean>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    fetchHandler.freeze()
                    func?.call(true)
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(false)
                }
            }
            return this
        }
    }

    override fun freeze(): Fetch {
        return freeze(null)
    }

    override fun unfreeze(func: Func<Boolean>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    fetchHandler.unfreeze()
                    func?.call(true)
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(false)
                }
            }
            return this
        }
    }

    override fun unfreeze(): Fetch {
        return unfreeze(null)
    }

    override fun resume(ids: List<Int>, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.resume(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Queued download $it")
                            listenerCoordinator.mainListener.onQueued(it, false)
                            logger.d("Resumed download $it")
                            listenerCoordinator.mainListener.onResumed(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun resume(ids: List<Int>): Fetch {
        return resume(ids, null)
    }

    override fun resume(id: Int, func2: Func2<Download?>?): Fetch {
        return resume(listOf(id), Func { func2?.call(it.firstOrNull()) })
    }

    override fun resume(id: Int): Fetch {
        return resume(id, null)
    }

    override fun resumeGroup(id: Int, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.resumeGroup(id)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Resumed download $it")
                            listenerCoordinator.mainListener.onResumed(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun resumeGroup(id: Int): Fetch {
        return resumeGroup(id, null)
    }

    override fun remove(ids: List<Int>, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.remove(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Removed download $it")
                            listenerCoordinator.mainListener.onRemoved(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun remove(ids: List<Int>): Fetch {
        return remove(ids, null)
    }

    override fun remove(id: Int): Fetch {
        return remove(id, null)
    }

    override fun remove(id: Int, func2: Func2<Download?>?): Fetch {
        return remove(listOf(id), Func { func2?.call(it.firstOrNull()) })
    }

    override fun removeGroup(id: Int, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.removeGroup(id)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Removed download $it")
                            listenerCoordinator.mainListener.onRemoved(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun removeGroup(id: Int): Fetch {
        return removeGroup(id, null)
    }

    override fun removeAll(func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.removeAll()
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Removed download $it")
                            listenerCoordinator.mainListener.onRemoved(it)
                        }
                    }
                    func?.call(downloads)
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun removeAll(): Fetch {
        return removeAll(null)
    }

    override fun removeAllWithStatus(status: Status, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.removeAllWithStatus(status)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Removed download $it")
                            listenerCoordinator.mainListener.onRemoved(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun removeAllWithStatus(status: Status): Fetch {
        return removeAllWithStatus(status, null)
    }

    override fun delete(ids: List<Int>, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.delete(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Deleted download $it")
                            listenerCoordinator.mainListener.onDeleted(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun delete(id: Int, func2: Func2<Download?>?): Fetch {
        return delete(listOf(id), Func { func2?.call(it.firstOrNull()) })
    }

    override fun deleteGroup(id: Int, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.deleteGroup(id)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Deleted download $it")
                            listenerCoordinator.mainListener.onDeleted(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun delete(ids: List<Int>): Fetch {
        return delete(ids, null)
    }

    override fun delete(id: Int): Fetch {
        return delete(id, null)
    }

    override fun deleteGroup(id: Int): Fetch {
        return deleteGroup(id, null)
    }

    override fun deleteAll(func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.deleteAll()
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Deleted download $it")
                            listenerCoordinator.mainListener.onDeleted(it)
                        }
                    }
                    func?.call(downloads)
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun deleteAll(): Fetch {
        return deleteAll(null)
    }

    override fun deleteAllWithStatus(status: Status, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.deleteAllWithStatus(status)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Deleted download $it")
                            listenerCoordinator.mainListener.onDeleted(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun deleteAllWithStatus(status: Status): Fetch {
        return deleteAllWithStatus(status, null)
    }

    override fun cancel(ids: List<Int>, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.cancel(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Cancelled download $it")
                            listenerCoordinator.mainListener.onCancelled(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun cancel(ids: List<Int>): Fetch {
        return cancel(ids, null)
    }

    override fun cancel(id: Int, func2: Func2<Download?>?): Fetch {
        return cancel(listOf(id), Func { func2?.call(it.firstOrNull()) })
    }

    override fun cancel(id: Int): Fetch {
        return cancel(id, null)
    }

    override fun cancelGroup(id: Int, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.cancelGroup(id)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Cancelled download $it")
                            listenerCoordinator.mainListener.onCancelled(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun cancelGroup(id: Int): Fetch {
        return cancelGroup(id, null)
    }

    override fun cancelAll(func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.cancelAll()
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Cancelled download $it")
                            listenerCoordinator.mainListener.onCancelled(it)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun cancelAll(): Fetch {
        return cancelAll(null)
    }

    override fun retry(ids: List<Int>, func: Func<List<Download>>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.retry(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Queued $it for download")
                            listenerCoordinator.mainListener.onQueued(it, false)
                        }
                        func?.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func?.call(emptyList())
                }
            }
            return this
        }
    }

    override fun retry(ids: List<Int>): Fetch {
        return retry(ids, null)
    }

    override fun retry(id: Int, func2: Func2<Download?>?): Fetch {
        return retry(listOf(id), Func { func2?.call(it.firstOrNull()) })
    }

    override fun retry(id: Int): Fetch {
        return retry(id, null)
    }

    override fun updateRequest(oldRequestId: Int, newRequest: Request, func: Func<Download>?,
                               func2: Func<Error>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val download = fetchHandler.updateRequest(oldRequestId, newRequest)
                    if (download != null && func != null) {
                        uiHandler.post {
                            func.call(download)
                        }
                    }
                    if (download == null && func2 != null) {
                        uiHandler.post {
                            func2.call(Error.DOWNLOAD_NOT_FOUND)
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Failed to update request with id $oldRequestId", e)
                    val error = getErrorFromMessage(e.message)
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

    override fun getDownloads(func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.getDownloads()
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func.call(emptyList())
                }
            }
            return this
        }
    }

    override fun getDownload(id: Int, func2: Func2<Download?>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val download = fetchHandler.getDownload(id)
                    uiHandler.post {
                        func2.call(download)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func2.call(null)
                }
            }
            return this
        }
    }

    override fun getDownloads(idList: List<Int>, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.getDownloads(idList)
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func.call(emptyList())
                }
            }
            return this
        }
    }

    override fun getDownloadsInGroup(groupId: Int, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.getDownloadsInGroup(groupId)
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func.call(emptyList())
                }
            }
            return this
        }
    }

    override fun getDownloadsWithStatus(status: Status, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.getDownloadsWithStatus(status)
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func.call(emptyList())
                }
            }
            return this
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, status: Status, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.getDownloadsInGroupWithStatus(groupId, status)
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func.call(emptyList())
                }
            }
            return this
        }
    }

    override fun getDownloadsByRequestIdentifier(identifier: Long, func: Func<List<Download>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.getDownloadsByRequestIdentifier(identifier)
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func.call(emptyList())
                }
            }
            return this
        }
    }

    override fun addCompletedDownload(completedDownload: CompletedDownload, func: Func<Download>?, func2: Func<Error>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val download = fetchHandler.enqueueCompletedDownload(completedDownload)
                    if (func != null) {
                        uiHandler.post {
                            func.call(download)
                        }
                    }
                    uiHandler.post {
                        listenerCoordinator.mainListener.onCompleted(download)
                        logger.d("Added CompletedDownload $download")
                    }
                } catch (e: Exception) {
                    logger.e("Failed to add CompletedDownload $completedDownload", e)
                    val error = getErrorFromMessage(e.message)
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

    override fun addCompletedDownloads(completedDownloads: List<CompletedDownload>, func: Func<List<Download>>?, func2: Func<Error>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.enqueueCompletedDownloads(completedDownloads)
                    if (func != null) {
                        uiHandler.post {
                            func.call(downloads)
                        }
                    }
                    uiHandler.post {
                        downloads.forEach {
                            listenerCoordinator.mainListener.onCompleted(it)
                            logger.d("Added CompletedDownload $it")
                        }
                    }
                } catch (e: Exception) {
                    logger.e("Failed to add CompletedDownload list $completedDownloads")
                    val error = getErrorFromMessage(e.message)
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

    override fun addListener(listener: FetchListener): Fetch {
        return addListener(listener, DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED)
    }

    override fun addListener(listener: FetchListener, notify: Boolean): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.addListener(listener, notify)
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

    override fun getDownloadBlocks(downloadId: Int, func: Func<List<DownloadBlock>>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloadBlocksList = fetchHandler.getDownloadBlocks(downloadId)
                    uiHandler.post {
                        func.call(downloadBlocksList)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                    func.call(emptyList())
                }
            }
            return this
        }
    }

    override fun setGlobalNetworkType(networkType: NetworkType): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    fetchHandler.setGlobalNetworkType(networkType)
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun enableLogging(enabled: Boolean): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    fetchHandler.enableLogging(enabled)
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
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
            handlerWrapper.post {
                try {
                    fetchHandler.close()
                } catch (e: Exception) {
                    logger.e("exception occurred whiles shutting down Fetch with namespace:$namespace", e)
                }
            }
        }
    }

    protected fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchException("This fetch instance has been closed. Create a new " +
                    "instance using the builder.",
                    FetchException.Code.CLOSED)
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(modules: Modules): FetchImpl {
            return FetchImpl(
                    namespace = modules.fetchConfiguration.namespace,
                    handlerWrapper = modules.handlerWrapper,
                    uiHandler = modules.uiHandler,
                    fetchHandler = modules.fetchHandler,
                    logger = modules.fetchConfiguration.logger,
                    listenerCoordinator = modules.listenerCoordinator)
        }

    }

}

