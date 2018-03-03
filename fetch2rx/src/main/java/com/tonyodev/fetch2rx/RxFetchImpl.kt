package com.tonyodev.fetch2rx

import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchHandler
import com.tonyodev.fetch2.fetch.FetchImpl
import com.tonyodev.fetch2.fetch.FetchModulesBuilder.Modules
import com.tonyodev.fetch2.provider.ListenerProvider
import com.tonyodev.fetch2.util.FAILED_TO_ENQUEUE_REQUEST
import com.tonyodev.fetch2.util.DOWNLOAD_NOT_FOUND
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

open class RxFetchImpl(namespace: String,
                       handler: Handler,
                       uiHandler: Handler,
                       fetchHandler: FetchHandler,
                       fetchListenerProvider: ListenerProvider,
                       logger: Logger)
    : FetchImpl(namespace, handler, uiHandler,
        fetchHandler, fetchListenerProvider, logger), RxFetch {

    protected val scheduler = AndroidSchedulers.from(handler.looper)
    protected val uiSceduler = AndroidSchedulers.mainThread()

    override fun enqueue(request: Request): Convertible<Download> {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            val flowable = Flowable.just(request)
                    .subscribeOn(scheduler)
                    .flatMap {
                        fetchHandler.throwExceptionIfClosed()
                        val download: Download
                        try {
                            download = fetchHandler.enqueue(it)
                            uiHandler.post {
                                fetchListenerProvider.mainListener.onQueued(download)
                                logger.d("Queued $download for download")
                            }
                        } catch (e: Exception) {
                            throw FetchException(e.message ?: FAILED_TO_ENQUEUE_REQUEST)
                        }
                        Flowable.just(download)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun enqueue(requests: List<Request>): Convertible<List<Download>> {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            val flowable = Flowable.just(requests)
                    .subscribeOn(scheduler)
                    .flatMap {
                        fetchHandler.throwExceptionIfClosed()
                        val downloads: List<Download>
                        try {
                            downloads = fetchHandler.enqueue(it)
                            uiHandler.post {
                                downloads.forEach {
                                    fetchListenerProvider.mainListener.onQueued(it)
                                    logger.d("Queued $it for download")
                                }
                            }
                        } catch (e: Exception) {
                            throw FetchException(e.message ?: FAILED_TO_ENQUEUE_REQUEST)
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun updateRequest(id: Int, requestInfo: RequestInfo): Convertible<Download> {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()

            val flowable = Flowable.just(Object())
                    .subscribeOn(scheduler)
                    .flatMap {
                        fetchHandler.throwExceptionIfClosed()
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
            fetchHandler.throwExceptionIfClosed()
            val flowable = Flowable.just(Object())
                    .subscribeOn(scheduler)
                    .flatMap {
                        fetchHandler.throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloads()
                        Flowable.just(downloads)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownloads(idList: List<Int>): Convertible<List<Download>> {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            val flowable = Flowable.just(idList)
                    .subscribeOn(scheduler)
                    .flatMap {
                        fetchHandler.throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloads(it)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownload(id: Int): Convertible<Download> {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            val flowable = Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        fetchHandler.throwExceptionIfClosed()
                        val download = fetchHandler.getDownload(it) ?:
                                throw FetchException(DOWNLOAD_NOT_FOUND)
                        Flowable.just(download)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownloadsInGroup(groupId: Int): Convertible<List<Download>> {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            val flowable = Flowable.just(groupId)
                    .subscribeOn(scheduler)
                    .flatMap {
                        fetchHandler.throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloadsInGroup(it)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownloadsWithStatus(status: Status): Convertible<List<Download>> {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            val flowable = Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        fetchHandler.throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloadsWithStatus(it)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiSceduler)
            return Convertible(flowable)
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): Convertible<List<Download>> {
        synchronized(lock) {
            fetchHandler.throwExceptionIfClosed()
            val flowable = Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        fetchHandler.throwExceptionIfClosed()
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
                    namespace = modules.prefs.namespace,
                    handler = modules.handler,
                    uiHandler = modules.uiHandler,
                    fetchHandler = modules.fetchHandler,
                    fetchListenerProvider = modules.fetchListenerProvider,
                    logger = modules.prefs.logger)
        }

    }

}