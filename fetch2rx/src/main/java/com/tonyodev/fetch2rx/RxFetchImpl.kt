package com.tonyodev.fetch2rx

import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchHandler
import com.tonyodev.fetch2.fetch.FetchImpl
import com.tonyodev.fetch2.fetch.FetchModulesBuilder.Modules
import com.tonyodev.fetch2core.HandlerWrapper
import com.tonyodev.fetch2.fetch.ListenerCoordinator
import com.tonyodev.fetch2core.FAILED_TO_ENQUEUE_REQUEST
import com.tonyodev.fetch2core.DOWNLOAD_NOT_FOUND
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.Logger
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers


open class RxFetchImpl(namespace: String,
                       handlerWrapper: HandlerWrapper,
                       uiHandler: Handler,
                       fetchHandler: FetchHandler,
                       logger: Logger,
                       listenerCoordinator: ListenerCoordinator)
    : FetchImpl(namespace, handlerWrapper, uiHandler,
        fetchHandler, logger, listenerCoordinator), RxFetch {

    protected val scheduler = AndroidSchedulers.from(handlerWrapper.getLooper())
    protected val uiSceduler = AndroidSchedulers.mainThread()

    override fun enqueue(request: Request): Convertible<Request> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val flowable = Flowable.just(request)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val download: Download
                        try {
                            download = fetchHandler.enqueue(it)
                            uiHandler.post {
                                listenerCoordinator.mainListener.onQueued(download, false)
                                logger.d("Queued $download for download")
                            }
                        } catch (e: Exception) {
                            throw FetchException(e.message ?: FAILED_TO_ENQUEUE_REQUEST)
                        }
                        Flowable.just(download.request)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun enqueue(requests: List<Request>): Convertible<List<Request>> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val flowable = Flowable.just(requests)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads: List<Download>
                        try {
                            downloads = fetchHandler.enqueue(it)
                            uiHandler.post {
                                downloads.forEach {
                                    listenerCoordinator.mainListener.onQueued(it, false)
                                    logger.d("Queued $it for download")
                                }
                            }
                        } catch (e: Exception) {
                            throw FetchException(e.message ?: FAILED_TO_ENQUEUE_REQUEST)
                        }
                        Flowable.just(downloads.map { it.request })
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun updateRequest(id: Int, requestInfo: RequestInfo): Convertible<Download> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val flowable = Flowable.just(Object())
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val download: Download?
                        try {
                            download = fetchHandler.updateRequest(id, requestInfo)
                        } catch (e: Exception) {
                            throw FetchException(e.message ?: FAILED_TO_ENQUEUE_REQUEST)
                        }
                        if (download == null) {
                            throw FetchException(DOWNLOAD_NOT_FOUND)
                        }
                        Flowable.just(download)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownloads(): Convertible<List<Download>> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val flowable = Flowable.just(Object())
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloads()
                        Flowable.just(downloads)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownloads(idList: List<Int>): Convertible<List<Download>> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val flowable = Flowable.just(idList)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloads(it)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownload(id: Int): Convertible<Download> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val flowable = Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val download = fetchHandler.getDownload(it)
                                ?: throw FetchException(DOWNLOAD_NOT_FOUND)
                        Flowable.just(download)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownloadsInGroup(groupId: Int): Convertible<List<Download>> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val flowable = Flowable.just(groupId)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloadsInGroup(it)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownloadsWithStatus(status: Status): Convertible<List<Download>> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val flowable = Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloadsWithStatus(it)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): Convertible<List<Download>> {
        synchronized(lock) {
            throwExceptionIfClosed()
            val flowable = Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloadsInGroupWithStatus(groupId, status)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
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