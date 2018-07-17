package com.tonyodev.fetch2rx

import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchHandler
import com.tonyodev.fetch2.fetch.FetchModulesBuilder.Modules
import com.tonyodev.fetch2.fetch.ListenerCoordinator
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED
import com.tonyodev.fetch2core.*
import com.tonyodev.fetch2rx.util.toConvertible
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers

open class RxFetchImpl(override val namespace: String,
                       private val handlerWrapper: HandlerWrapper,
                       private val uiHandler: Handler,
                       private val fetchHandler: FetchHandler,
                       private val logger: Logger,
                       private val listenerCoordinator: ListenerCoordinator) : RxFetch {

    private val scheduler = AndroidSchedulers.from(handlerWrapper.getLooper())
    private val uiSceduler = AndroidSchedulers.mainThread()
    private val lock = Object()
    @Volatile
    private var closed = false
    override val isClosed: Boolean
        get() = closed

    init {
        handlerWrapper.post {
            fetchHandler.init()
        }
    }

    override fun enqueue(request: Request): Convertible<Request> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(request)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val download: Download
                        try {
                            download = fetchHandler.enqueue(it)
                            uiHandler.post {
                                listenerCoordinator.mainListener.onAdded(download)
                                logger.d("Added Download $download")
                                if (download.status == Status.QUEUED) {
                                    listenerCoordinator.mainListener.onQueued(download, false)
                                    logger.d("Queued $download for download")
                                }
                            }
                        } catch (e: Exception) {
                            throw FetchException(e.message ?: FAILED_TO_ENQUEUE_REQUEST)
                        }
                        Flowable.just(download.request)
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun enqueue(requests: List<Request>): Convertible<List<Request>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(requests)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads: List<Download>
                        try {
                            downloads = fetchHandler.enqueue(it)
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
                            throw FetchException(e.message ?: FAILED_TO_ENQUEUE_REQUEST)
                        }
                        Flowable.just(downloads.map { it.request })
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun pause(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.pause(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Paused download $it")
                                    listenerCoordinator.mainListener.onPaused(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun pause(id: Int): Convertible<Download?> {
        return pause(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    Flowable.just(it.firstOrNull())
                }
                .observeOn(uiSceduler)
                .toConvertible()
    }

    override fun pauseGroup(id: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.pausedGroup(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Paused download $it")
                                    listenerCoordinator.mainListener.onPaused(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun freeze(): Convertible<Boolean> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            fetchHandler.freeze()
                            Flowable.just(true)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(false)
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun unfreeze(): Convertible<Boolean> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            fetchHandler.unfreeze()
                            Flowable.just(true)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(false)
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun resume(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.resume(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Queued download $it")
                                    listenerCoordinator.mainListener.onQueued(it, false)
                                    logger.d("Resumed download $it")
                                    listenerCoordinator.mainListener.onResumed(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun resume(id: Int): Convertible<Download?> {
        return resume(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    Flowable.just(it.firstOrNull())
                }
                .observeOn(uiSceduler)
                .toConvertible()
    }

    override fun resumeGroup(id: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.resumeGroup(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Resumed download $it")
                                    listenerCoordinator.mainListener.onResumed(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun remove(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.remove(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Removed download $it")
                                    listenerCoordinator.mainListener.onRemoved(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun remove(id: Int): Convertible<Download?> {
        return remove(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    Flowable.just(it.firstOrNull())
                }
                .observeOn(uiSceduler)
                .toConvertible()
    }

    override fun removeGroup(id: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.removeGroup(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Removed download $it")
                                    listenerCoordinator.mainListener.onRemoved(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun removeAll(): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.removeAll()
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Removed download $it")
                                    listenerCoordinator.mainListener.onRemoved(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun removeAllWithStatus(status: Status): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.removeAllWithStatus(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Removed download $it")
                                    listenerCoordinator.mainListener.onRemoved(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun removeAllInGroupWithStatus(id: Int, status: Status): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Pair(id, status))
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.removeAllInGroupWithStatus(it.first, it.second)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Removed download $it")
                                    listenerCoordinator.mainListener.onRemoved(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun delete(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.delete(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Deleted download $it")
                                    listenerCoordinator.mainListener.onDeleted(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun delete(id: Int): Convertible<Download?> {
        return delete(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    Flowable.just(it.firstOrNull())
                }
                .observeOn(uiSceduler)
                .toConvertible()
    }

    override fun deleteGroup(id: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.deleteGroup(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Deleted download $it")
                                    listenerCoordinator.mainListener.onDeleted(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun deleteAll(): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.deleteAll()
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Deleted download $it")
                                    listenerCoordinator.mainListener.onDeleted(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun deleteAllWithStatus(status: Status): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.deleteAllWithStatus(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Deleted download $it")
                                    listenerCoordinator.mainListener.onDeleted(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun deleteAllInGroupWithStatus(id: Int, status: Status): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Pair(id, status))
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.deleteAllInGroupWithStatus(it.first, it.second)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Deleted download $it")
                                    listenerCoordinator.mainListener.onDeleted(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun cancel(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.cancel(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Cancelled download $it")
                                    listenerCoordinator.mainListener.onCancelled(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun cancel(id: Int): Convertible<Download?> {
        return cancel(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    Flowable.just(it.firstOrNull())
                }
                .observeOn(uiSceduler)
                .toConvertible()
    }

    override fun cancelGroup(id: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.cancelGroup(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Cancelled download $it")
                                    listenerCoordinator.mainListener.onCancelled(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun cancelAll(): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.cancelAll()
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Cancelled download $it")
                                    listenerCoordinator.mainListener.onCancelled(it)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun retry(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.retry(it)
                            uiHandler.post {
                                downloads.forEach {
                                    logger.d("Queued $it for download")
                                    listenerCoordinator.mainListener.onQueued(it, false)
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun retry(id: Int): Convertible<Download?> {
        return retry(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    Flowable.just(it.firstOrNull())
                }
                .observeOn(uiSceduler)
                .toConvertible()
    }

    override fun addListener(listener: FetchListener): RxFetch {
        return addListener(listener, DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED)
    }

    override fun addListener(listener: FetchListener, notify: Boolean): RxFetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.addListener(listener, notify)
            }
            return this
        }
    }

    override fun removeListener(listener: FetchListener): RxFetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.removeListener(listener)
            }
            return this
        }
    }

    override fun updateRequest(oldRequestId: Int, newRequest: Request): Convertible<Download?> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val download = fetchHandler.updateRequest(oldRequestId, newRequest)
                            Flowable.just(download)
                        } catch (e: Exception) {
                            logger.e("Failed to update request with id $oldRequestId", e)
                            throw FetchException(e.message
                                    ?: FAILED_TO_UPDATE_REQUEST, FetchException.Code.REQUEST_NOT_UPDATED)
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun getDownloads(): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.getDownloads()
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun getDownloads(idList: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(idList)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.getDownloads(idList)
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())

                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun getDownload(id: Int): Convertible<Download?> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val download = fetchHandler.getDownload(id)
                            Flowable.just(download)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(null)
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun getDownloadsInGroup(groupId: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(groupId)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.getDownloadsInGroup(groupId)
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun getDownloadsWithStatus(status: Status): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.getDownloadsWithStatus(status)
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.getDownloadsInGroupWithStatus(groupId, status)
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun getDownloadsByRequestIdentifier(identifier: Long): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(identifier)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.getDownloadsByRequestIdentifier(identifier)
                            Flowable.just(downloads)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<Download>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun addCompletedDownload(completedDownload: CompletedDownload): Convertible<Download?> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(completedDownload)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val download = fetchHandler.enqueueCompletedDownload(completedDownload)
                            uiHandler.post {
                                listenerCoordinator.mainListener.onCompleted(download)
                                logger.d("Added CompletedDownload $download")
                            }
                            Flowable.just(download)
                        } catch (e: Exception) {
                            logger.e("Failed to add CompletedDownload $completedDownload", e)
                            throw  FetchException(e.message
                                    ?: FAILED_TO_ADD_COMPLETED_DOWNLOAD, FetchException.Code.COMPLETED_DOWNLOAD_NOT_ADDED)
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun addCompletedDownloads(completedDownloads: List<CompletedDownload>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(completedDownloads)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloads = fetchHandler.enqueueCompletedDownloads(completedDownloads)
                            uiHandler.post {
                                downloads.forEach {
                                    listenerCoordinator.mainListener.onCompleted(it)
                                    logger.d("Added CompletedDownload $it")
                                }
                            }
                            Flowable.just(downloads)
                        } catch (e: Exception) {
                            logger.e("Failed to add CompletedDownload list $completedDownloads")
                            throw  FetchException(e.message
                                    ?: FAILED_TO_ADD_COMPLETED_DOWNLOAD, FetchException.Code.COMPLETED_DOWNLOAD_NOT_ADDED)
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun getDownloadBlocks(downloadId: Int): Convertible<List<DownloadBlock>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(downloadId)
                    .subscribeOn(scheduler)
                    .flatMap {
                        try {
                            val downloadBlocksList = fetchHandler.getDownloadBlocks(downloadId)
                            Flowable.just(downloadBlocksList)
                        } catch (e: FetchException) {
                            logger.e("Fetch with namespace $namespace error", e)
                            Flowable.just(emptyList<DownloadBlock>())
                        }
                    }
                    .observeOn(uiSceduler)
                    .toConvertible()
        }
    }

    override fun enableLogging(enabled: Boolean): RxFetch {
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

    override fun setGlobalNetworkType(networkType: NetworkType): RxFetch {
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

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchException("This fetch instance has been closed. Create a new " +
                    "instance using the builder.",
                    FetchException.Code.CLOSED)
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(modules: Modules): RxFetchImpl {
            return RxFetchImpl(
                    namespace = modules.fetchConfiguration.namespace,
                    handlerWrapper = modules.handlerWrapper,
                    uiHandler = modules.uiHandler,
                    fetchHandler = modules.fetchHandler,
                    logger = modules.fetchConfiguration.logger,
                    listenerCoordinator = modules.listenerCoordinator)
        }

    }

}