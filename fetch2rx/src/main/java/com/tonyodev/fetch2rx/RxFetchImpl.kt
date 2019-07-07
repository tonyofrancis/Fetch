package com.tonyodev.fetch2rx

import android.os.Handler
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchHandler
import com.tonyodev.fetch2.fetch.FetchModulesBuilder.Modules
import com.tonyodev.fetch2.fetch.ListenerCoordinator
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.FetchDatabaseManagerWrapper
import com.tonyodev.fetch2.util.ActiveDownloadInfo
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_AUTOSTART_ON_ATTACHED
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED
import com.tonyodev.fetch2.util.toDownloadInfo
import com.tonyodev.fetch2core.*
import com.tonyodev.fetch2rx.util.toConvertible
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers

open class RxFetchImpl(override val namespace: String,
                       override val fetchConfiguration: FetchConfiguration,
                       private val handlerWrapper: HandlerWrapper,
                       private val uiHandler: Handler,
                       private val fetchHandler: FetchHandler,
                       private val logger: Logger,
                       private val listenerCoordinator: ListenerCoordinator,
                       private val fetchDatabaseManagerWrapper: FetchDatabaseManagerWrapper) : RxFetch {

    private val scheduler = AndroidSchedulers.from(handlerWrapper.getLooper())
    private val uiScheduler = AndroidSchedulers.mainThread()
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

    override fun enqueue(request: Request): Convertible<Request> {
        return enqueue(listOf(request))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    if (it.isNotEmpty()) {
                        val enqueuedPair = it.first()
                        if (enqueuedPair.second != Error.NONE) {
                            val throwable = enqueuedPair.second.throwable
                            if (throwable != null) {
                                throw throwable
                            } else {
                                throw FetchException(ENQUEUE_NOT_SUCCESSFUL)
                            }
                        } else {
                            Flowable.just(enqueuedPair.first)
                        }
                    } else {
                        throw FetchException(ENQUEUE_NOT_SUCCESSFUL)
                    }
                }
                .observeOn(uiScheduler)
                .toConvertible()
    }

    override fun enqueue(requests: List<Request>): Convertible<List<Pair<Request, Error>>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(requests)
                    .subscribeOn(scheduler)
                    .flatMap { requests ->
                        throwExceptionIfClosed()
                        val distinctCount = requests.distinctBy { it.file }.count()
                        if (distinctCount != requests.size) {
                            throw FetchException(ENQUEUED_REQUESTS_ARE_NOT_DISTINCT)
                        }
                        val downloadPairs = fetchHandler.enqueue(requests)
                        uiHandler.post {
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
                        }
                        Flowable.just(downloadPairs.map { Pair(it.first.request, it.second) })
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }


    override fun pause(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap { ids ->
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.pause(ids)
                        uiHandler.post {
                            downloads.forEach {
                                logger.d("Paused download $it")
                                listenerCoordinator.mainListener.onPaused(it)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun pause(id: Int): Convertible<Download> {
        return pause(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    if (it.isNotEmpty()) {
                        Flowable.just(it.first())
                    } else {
                        throw FetchException(REQUEST_DOES_NOT_EXIST)
                    }
                }
                .observeOn(uiScheduler)
                .toConvertible()
    }

    override fun pauseGroup(id: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.pausedGroup(it)
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Paused download $download")
                                listenerCoordinator.mainListener.onPaused(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun freeze(): Convertible<Boolean> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        fetchHandler.freeze()
                        Flowable.just(true)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun unfreeze(): Convertible<Boolean> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        fetchHandler.unfreeze()
                        Flowable.just(true)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun resume(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap { ids ->
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.resume(ids)
                        uiHandler.post {
                            downloads.forEach {
                                logger.d("Queued download $it")
                                listenerCoordinator.mainListener.onQueued(it, false)
                                logger.d("Resumed download $it")
                                listenerCoordinator.mainListener.onResumed(it)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun resume(id: Int): Convertible<Download> {
        return resume(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    if (it.isNotEmpty()) {
                        Flowable.just(it.first())
                    } else {
                        throw FetchException(REQUEST_DOES_NOT_EXIST)
                    }
                }
                .observeOn(uiScheduler)
                .toConvertible()
    }

    override fun resumeGroup(id: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.resumeGroup(it)
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Queued download $download")
                                listenerCoordinator.mainListener.onQueued(download, false)
                                logger.d("Resumed download $download")
                                listenerCoordinator.mainListener.onResumed(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun remove(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.remove(it)
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Removed download $download")
                                listenerCoordinator.mainListener.onRemoved(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun remove(id: Int): Convertible<Download> {
        return remove(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    if (it.isNotEmpty()) {
                        Flowable.just(it.first())
                    } else {
                        throw FetchException(REQUEST_DOES_NOT_EXIST)
                    }
                }
                .observeOn(uiScheduler)
                .toConvertible()
    }

    override fun removeGroup(id: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.removeGroup(it)
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Removed download $download")
                                listenerCoordinator.mainListener.onRemoved(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun removeAll(): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.removeAll()
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Removed download $download")
                                listenerCoordinator.mainListener.onRemoved(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun removeAllWithStatus(status: Status): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.removeAllWithStatus(it)
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Removed download $download")
                                listenerCoordinator.mainListener.onRemoved(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun removeAllInGroupWithStatus(id: Int, status: List<Status>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Pair(id, status))
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.removeAllInGroupWithStatus(it.first, it.second)
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Removed download $download")
                                listenerCoordinator.mainListener.onRemoved(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun delete(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap { ids ->
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.delete(ids)
                        uiHandler.post {
                            downloads.forEach {
                                logger.d("Deleted download $it")
                                listenerCoordinator.mainListener.onDeleted(it)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun delete(id: Int): Convertible<Download> {
        return delete(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    if (it.isNotEmpty()) {
                        Flowable.just(it.first())
                    } else {
                        throw  FetchException(REQUEST_DOES_NOT_EXIST)
                    }
                }
                .observeOn(uiScheduler)
                .toConvertible()
    }

    override fun deleteGroup(id: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap { id ->
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.deleteGroup(id)
                        uiHandler.post {
                            downloads.forEach {
                                logger.d("Deleted download $it")
                                listenerCoordinator.mainListener.onDeleted(it)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun deleteAll(): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.deleteAll()
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Deleted download $download")
                                listenerCoordinator.mainListener.onDeleted(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun deleteAllWithStatus(status: Status): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.deleteAllWithStatus(it)
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Deleted download $download")
                                listenerCoordinator.mainListener.onDeleted(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun deleteAllInGroupWithStatus(id: Int, statuses: List<Status>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Pair(id, statuses))
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.deleteAllInGroupWithStatus(it.first, it.second)
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Deleted download $download")
                                listenerCoordinator.mainListener.onDeleted(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun cancel(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap { ids ->
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.cancel(ids)
                        uiHandler.post {
                            downloads.forEach {
                                logger.d("Cancelled download $it")
                                listenerCoordinator.mainListener.onCancelled(it)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun cancel(id: Int): Convertible<Download> {
        return cancel(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    if (it.isNotEmpty()) {
                        Flowable.just(it.first())
                    } else {
                        throw FetchException(REQUEST_DOES_NOT_EXIST)
                    }
                }
                .observeOn(uiScheduler)
                .toConvertible()
    }

    override fun cancelGroup(id: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap { id ->
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.cancelGroup(id)
                        uiHandler.post {
                            downloads.forEach {
                                logger.d("Cancelled download $it")
                                listenerCoordinator.mainListener.onCancelled(it)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun cancelAll(): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.cancelAll()
                        uiHandler.post {
                            downloads.forEach { download ->
                                logger.d("Cancelled download $download")
                                listenerCoordinator.mainListener.onCancelled(download)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun retry(ids: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(ids)
                    .subscribeOn(scheduler)
                    .flatMap { ids ->
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.retry(ids)
                        uiHandler.post {
                            downloads.forEach {
                                logger.d("Queued $it for download")
                                listenerCoordinator.mainListener.onQueued(it, false)
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun retry(id: Int): Convertible<Download> {
        return retry(listOf(id))
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    if (it.isNotEmpty()) {
                        Flowable.just(it.first())
                    } else {
                        throw FetchException(REQUEST_DOES_NOT_EXIST)
                    }
                }
                .observeOn(uiScheduler)
                .toConvertible()
    }

    override fun resetAutoRetryAttempts(downloadId: Int, retryDownload: Boolean): Convertible<Download?> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(downloadId)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val download = fetchHandler.resetAutoRetryAttempts(downloadId, retryDownload)
                        uiHandler.post {
                            if (download != null && download.status == Status.QUEUED) {
                                logger.d("Queued $download for download")
                                listenerCoordinator.mainListener.onQueued(download, false)
                            }
                        }
                        Flowable.just(download)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun addListener(listener: FetchListener): RxFetch {
        return addListener(listener, DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED)
    }

    override fun addListener(listener: FetchListener, notify: Boolean): RxFetch {
        return addListener(listener, notify, DEFAULT_ENABLE_LISTENER_AUTOSTART_ON_ATTACHED)
    }

    override fun addListener(listener: FetchListener, notify: Boolean, autoStart: Boolean): RxFetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.addListener(listener, notify, autoStart)
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

    override fun getListenerSet(): Set<FetchListener> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            fetchHandler.getListenerSet()
        }
    }

    override fun updateRequest(requestId: Int, updatedRequest: Request, notifyListeners: Boolean): Convertible<Download> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Pair(requestId, updatedRequest))
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloadPair = fetchHandler.updateRequest(it.first, it.second)
                        val download = downloadPair.first
                        uiHandler.post {
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
                        }
                        Flowable.just(download)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun renameCompletedDownloadFile(id: Int, newFileName: String): Convertible<Download> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Pair(id, newFileName))
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val download = fetchHandler.renameCompletedDownloadFile(it.first, it.second)
                        Flowable.just(download)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun replaceExtras(id: Int, extras: Extras): Convertible<Download> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Pair(id, extras))
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val download = fetchHandler.replaceExtras(it.first, it.second)
                        Flowable.just(download)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getDownloads(): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloads()
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getDownloads(idList: List<Int>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(idList)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloads(idList)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getDownload(id: Int): Convertible<Download?> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(id)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val download = fetchHandler.getDownload(id)
                        Flowable.just(download)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getDownloadsInGroup(groupId: Int): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(groupId)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloadsInGroup(groupId)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getDownloadsWithStatus(status: Status): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloadsWithStatus(status)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getDownloadsInGroupWithStatus(groupId: Int, status: List<Status>): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(status)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloadsInGroupWithStatus(groupId, status)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getDownloadsByRequestIdentifier(identifier: Long): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(identifier)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloadsByRequestIdentifier(identifier)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getFetchGroup(group: Int): Convertible<FetchGroup> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(group)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val fetchGroup = fetchHandler.getFetchGroup(group)
                        Flowable.just(fetchGroup)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getAllGroupIds(): Convertible<List<Int>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Any())
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val fetchGroupIdList = fetchHandler.getAllGroupIds()
                        Flowable.just(fetchGroupIdList)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getDownloadsByTag(tag: String): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(tag)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.getDownloadsByTag(it)
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun addCompletedDownload(completedDownload: CompletedDownload, alertListeners: Boolean): Convertible<Download> {
        return addCompletedDownloads(listOf(completedDownload), alertListeners)
                .flowable
                .subscribeOn(scheduler)
                .flatMap {
                    if (it.isNotEmpty()) {
                        Flowable.just(it.first())
                    } else {
                        throw FetchException(FAILED_TO_ADD_COMPLETED_DOWNLOAD)
                    }
                }
                .observeOn(uiScheduler)
                .toConvertible()
    }

    override fun addCompletedDownloads(completedDownloads: List<CompletedDownload>, alertListeners: Boolean): Convertible<List<Download>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(completedDownloads)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloads = fetchHandler.enqueueCompletedDownloads(completedDownloads)
                        uiHandler.post {
                            downloads.forEach { download ->
                                if (alertListeners) {
                                    listenerCoordinator.mainListener.onCompleted(download)
                                }
                                logger.d("Added CompletedDownload $download")
                            }
                        }
                        Flowable.just(downloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getDownloadBlocks(downloadId: Int): Convertible<List<DownloadBlock>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(downloadId)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val downloadBlocksList = fetchHandler.getDownloadBlocks(downloadId)
                        Flowable.just(downloadBlocksList)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun hasActiveDownloads(includeAddedDownloads: Boolean): Convertible<Boolean> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(includeAddedDownloads)
                    .subscribeOn(scheduler)
                    .flatMap {
                        throwExceptionIfClosed()
                        val hasActiveDownloads = fetchHandler.hasActiveDownloads(it)
                        Flowable.just(hasActiveDownloads)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getContentLengthForRequest(request: Request, fromServer: Boolean): Convertible<Long> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Pair(request, fromServer))
                    .subscribeOn(AndroidSchedulers.from(handlerWrapper.getWorkTaskLooper()))
                    .flatMap {
                        throwExceptionIfClosed()
                        val contentLength = fetchHandler.getContentLengthForRequest(it.first, it.second)
                        Flowable.just(contentLength)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getContentLengthForRequests(requests: List<Request>, fromServer: Boolean): Convertible<Pair<List<Pair<Request, Long>>, List<Pair<Request, Error>>>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Pair(requests, fromServer))
                    .subscribeOn(AndroidSchedulers.from(handlerWrapper.getWorkTaskLooper()))
                    .flatMap {
                        throwExceptionIfClosed()
                        val results = mutableListOf<Pair<Request, Long>>()
                        val results2 = mutableListOf<Pair<Request, Error>>()
                        for (request in requests) {
                            try {
                                results.add(Pair(request, fetchHandler.getContentLengthForRequest(request, fromServer)))
                            } catch (e: Exception) {
                                logger.e("RxFetch with namespace $namespace error", e)
                                val error = getErrorFromMessage(e.message)
                                error.throwable = e
                                results2.add(Pair(request, error))
                            }
                        }
                        Flowable.just(Pair(results as List<Pair<Request, Long>>, results2 as List<Pair<Request, Error>>))
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getServerResponse(url: String, headers: Map<String, String>?): Convertible<Downloader.Response> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(Pair(url, headers))
                    .subscribeOn(AndroidSchedulers.from(handlerWrapper.getWorkTaskLooper()))
                    .flatMap {
                        throwExceptionIfClosed()
                        val contentLength = fetchHandler.getServerResponse(url, headers)
                        Flowable.just(contentLength)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun getFetchFileServerCatalog(request: Request): Convertible<List<FileResource>> {
        return synchronized(lock) {
            throwExceptionIfClosed()
            Flowable.just(request)
                    .subscribeOn(AndroidSchedulers.from(handlerWrapper.getWorkTaskLooper()))
                    .flatMap {
                        throwExceptionIfClosed()
                        val catalogList = fetchHandler.getFetchFileServerCatalog(request)
                        Flowable.just(catalogList)
                    }
                    .observeOn(uiScheduler)
                    .toConvertible()
        }
    }

    override fun enableLogging(enabled: Boolean): RxFetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.enableLogging(enabled)
            }
            return this
        }
    }

    override fun setGlobalNetworkType(networkType: NetworkType): RxFetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.setGlobalNetworkType(networkType)
            }
            return this
        }
    }

    override fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int): RxFetch {
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
                    logger.e("exception occurred whiles shutting down RxFetch with namespace:$namespace", e)
                }
            }
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw FetchException("This rxFetch instance has been closed. Create a new " +
                    "instance using the builder.")
        }
    }

    override fun awaitFinishOrTimeout(allowTimeInMilliseconds: Long) {
        com.tonyodev.fetch2.util.awaitFinishOrTimeout(allowTimeInMilliseconds, fetchHandler)
    }

    override fun awaitFinish() {
        awaitFinishOrTimeout(-1)
    }

    override fun attachFetchObserversForDownload(downloadId: Int, vararg fetchObservers: FetchObserver<Download>): RxFetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.addFetchObserversForDownload(downloadId, *fetchObservers)
            }
            return this
        }
    }

    override fun removeFetchObserversForDownload(downloadId: Int, vararg fetchObservers: FetchObserver<Download>): RxFetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                fetchHandler.removeFetchObserversForDownload(downloadId, *fetchObservers)
            }
            return this
        }
    }

    override fun addActiveDownloadsObserver(includeAddedDownloads: Boolean, fetchObserver: FetchObserver<Boolean>): RxFetch {
        synchronized(lock) {
            throwExceptionIfClosed()
            handlerWrapper.post {
                activeDownloadsSet.add(ActiveDownloadInfo(fetchObserver, includeAddedDownloads))
            }
            return this
        }
    }

    override fun removeActiveDownloadsObserver(fetchObserver: FetchObserver<Boolean>): RxFetch {
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
        fun newInstance(modules: Modules): RxFetchImpl {
            return RxFetchImpl(
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