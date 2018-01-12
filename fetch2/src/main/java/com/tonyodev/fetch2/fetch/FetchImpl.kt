package com.tonyodev.fetch2.fetch


import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.provider.ListenerProvider
import com.tonyodev.fetch2.getErrorFromMessage
import com.tonyodev.fetch2.fetch.FetchModulesBuilder.Modules


open class FetchImpl constructor(override val namespace: String,
                                 val handler: Handler,
                                 val uiHandler: Handler,
                                 val fetchHandler: FetchHandler,
                                 val fetchListenerProvider: ListenerProvider,
                                 val logger: Logger) : Fetch {

    val lock = Object()
    override val isClosed: Boolean
        get() = fetchHandler.isClosed

    init {
        handler.post {
            fetchHandler.init()
        }
    }

    override fun enqueue(request: Request, func: Func<Download>?, func2: Func<Error>?) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val download = fetchHandler.enqueue(request)
                    if (func != null) {
                        uiHandler.post {
                            func.call(download)
                        }
                    }
                    uiHandler.post {
                        fetchListenerProvider.mainListener.onQueued(download)
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
        }
    }

    override fun enqueue(requests: List<Request>, func: Func<List<Download>>?, func2: Func<Error>?) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.enqueue(requests)
                    if (func != null) {
                        uiHandler.post {
                            func.call(downloads)
                        }
                    }
                    uiHandler.post {
                        downloads.forEach {
                            fetchListenerProvider.mainListener.onQueued(it)
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
        }
    }

    override fun pause(vararg ids: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.pause(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Paused download $it")
                            fetchListenerProvider.mainListener.onPaused(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun pauseGroup(id: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.pausedGroup(id)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Paused download $it")
                            fetchListenerProvider.mainListener.onPaused(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun freeze() {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    fetchHandler.freeze()
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun unfreeze() {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    fetchHandler.unfreeze()
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun resume(vararg ids: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.resume(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Resumed download $it")
                            fetchListenerProvider.mainListener.onResumed(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun resumeGroup(id: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.resumeGroup(id)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Resumed download $it")
                            fetchListenerProvider.mainListener.onResumed(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun remove(vararg ids: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.remove(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Removed download $it")
                            fetchListenerProvider.mainListener.onRemoved(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun removeGroup(id: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.removeGroup(id)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Removed download $it")
                            fetchListenerProvider.mainListener.onRemoved(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun removeAll() {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.removeAll()
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Removed download $it")
                            fetchListenerProvider.mainListener.onRemoved(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun delete(vararg ids: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.delete(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Deleted download $it")
                            fetchListenerProvider.mainListener.onDeleted(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun deleteGroup(id: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.deleteGroup(id)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Deleted download $it")
                            fetchListenerProvider.mainListener.onDeleted(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun deleteAll() {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.deleteAll()
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Deleted download $it")
                            fetchListenerProvider.mainListener.onDeleted(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun cancel(vararg ids: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.cancel(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Cancelled download $it")
                            fetchListenerProvider.mainListener.onCancelled(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun cancelGroup(id: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.cancelGroup(id)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Cancelled download $it")
                            fetchListenerProvider.mainListener.onCancelled(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }


    override fun cancelAll() {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.cancelAll()
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Cancelled download $it")
                            fetchListenerProvider.mainListener.onCancelled(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun retry(vararg ids: Int) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.retry(ids)
                    uiHandler.post {
                        downloads.forEach {
                            logger.d("Queued $it for download")
                            fetchListenerProvider.mainListener.onQueued(it)
                        }
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun updateRequest(id: Int, requestInfo: RequestInfo, func: Func<Download>?,
                               func2: Func<Error>?) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
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
        }
    }

    override fun getDownloads(func: Func<List<Download>>) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.getDownloads()
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun getDownload(id: Int, func: Func2<Download?>) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val download = fetchHandler.getDownload(id)
                    uiHandler.post {
                        func.call(download)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun getDownloads(idList: List<Int>, func: Func<List<Download>>) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.getDownloads(idList)
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun getDownloadsInGroup(groupId: Int, func: Func<List<Download>>) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.getDownloadsInGroup(groupId)
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun getDownloadsWithStatus(status: Status, func: Func<List<Download>>) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.getDownloadsWithStatus(status)
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, status: Status, func: Func<List<Download>>) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    val downloads = fetchHandler.getDownloadsInGroupWithStatus(groupId, status)
                    uiHandler.post {
                        func.call(downloads)
                    }
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun addListener(listener: FetchListener) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            fetchHandler.addListener(listener)
        }
    }

    override fun removeListener(listener: FetchListener) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            fetchHandler.removeListener(listener)
        }
    }

    override fun setGlobalNetworkType(networkType: NetworkType) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    fetchHandler.setGlobalNetworkType(networkType)
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun enableLogging(enabled: Boolean) {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            handler.post {
                try {
                    fetchHandler.enableLogging(enabled)
                } catch (e: FetchException) {
                    logger.e("Fetch with namespace $namespace error", e)
                }
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            if (isClosed) {
                return
            }
            logger.d("$namespace closing/shutting down")
            try {
                fetchHandler.close()
            } catch (e: Exception) {
                logger.e("exception occurred whiles shutting down Fetch with namespace:$namespace", e)
            }
        }
    }

    companion object {
        fun newInstance(modules: Modules): FetchImpl {
            return FetchImpl(
                    namespace = modules.prefs.namespace,
                    handler = modules.handler,
                    uiHandler = modules.uiHandler,
                    fetchHandler = modules.fetchHandler,
                    fetchListenerProvider = modules.fetchListenerProvider,
                    logger = modules.prefs.logger)
        }
    }

}

