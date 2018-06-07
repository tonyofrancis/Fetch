package com.tonyodev.fetch2.fetch


import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.getErrorFromMessage
import com.tonyodev.fetch2.fetch.FetchModulesBuilder.Modules
import com.tonyodev.fetch2core.Func
import com.tonyodev.fetch2core.Func2
import com.tonyodev.fetch2core.HandlerWrapper
import com.tonyodev.fetch2core.Logger

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
                        listenerCoordinator.mainListener.onQueued(download, false)
                        logger.d("Queued $download for download")
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
                            listenerCoordinator.mainListener.onQueued(it, false)
                            logger.d("Queued $it for download")
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

    override fun pause(vararg ids: Int): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun pauseGroup(id: Int): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun freeze(): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    fetchHandler.freeze()
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun unfreeze(): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    fetchHandler.unfreeze()
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun resume(vararg ids: Int): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val downloads = fetchHandler.resume(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Resumed download $it")
                            listenerCoordinator.mainListener.onResumed(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun resumeGroup(id: Int): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun remove(vararg ids: Int): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun removeGroup(id: Int): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun removeAll(): Fetch {
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
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun removeAllWithStatus(status: Status): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun delete(vararg ids: Int): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun deleteGroup(id: Int): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun deleteAll(): Fetch {
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
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun deleteAllWithStatus(status: Status): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun cancel(vararg ids: Int): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun cancelGroup(id: Int): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }


    override fun cancelAll(): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun retry(vararg ids: Int): Fetch {
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
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
            return this
        }
    }

    override fun updateRequest(id: Int, requestInfo: RequestInfo, func: Func<Download>?,
                               func2: Func<Error>?): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val download = fetchHandler.updateRequest(id, requestInfo)
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
                    logger.e("Failed to update request with id $id", e)
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
                }
            }
            return this
        }
    }

    override fun getDownload(id: Int, func: Func2<Download?>): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                try {
                    val download = fetchHandler.getDownload(id)
                    uiHandler.post {
                        func.call(download)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
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
                }
            }
            return this
        }
    }

    override fun addListener(listener: FetchListener): Fetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.addListener(listener)
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

