package com.tonyodev.fetch2.fetch

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.database.DatabaseManager
import com.tonyodev.fetch2.database.DatabaseManagerImpl
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2.downloader.DownloadManagerImpl
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.helper.DownloadInfoManagerDelegate
import com.tonyodev.fetch2.helper.DownloadInfoUpdater
import com.tonyodev.fetch2.helper.PriorityQueueProcessor
import com.tonyodev.fetch2.helper.PriorityQueueProcessorImpl
import com.tonyodev.fetch2.provider.DownloadProvider
import com.tonyodev.fetch2.provider.ListenerProvider
import com.tonyodev.fetch2.provider.NetworkProvider
import com.tonyodev.fetch2.util.FETCH_ALREADY_EXIST
import com.tonyodev.fetch2.util.defaultDownloader
import java.lang.ref.WeakReference

object FetchModulesBuilder {

    private val lock = Object()
    private val activeFetchHandlerPool: MutableMap<String, WeakReference<FetchHandler>> = hashMapOf()

    fun buildModulesFromPrefs(prefs: FetchBuilderPrefs): Modules {
        synchronized(lock) {
            val ref = activeFetchHandlerPool[prefs.namespace]?.get()
            if (ref != null) {
                throw FetchException("Namespace:${prefs.namespace} $FETCH_ALREADY_EXIST",
                        FetchException.Code.FETCH_INSTANCE_WITH_NAMESPACE_ALREADY_EXIST)
            }
            val modules = Modules(prefs)
            activeFetchHandlerPool[prefs.namespace] = WeakReference(modules.fetchHandler)
            return modules
        }
    }

    fun removeActiveFetchHandlerNamespaceInstance(namespace: String) {
        synchronized(lock) {
            activeFetchHandlerPool.remove(namespace)
        }
    }

    open class Modules constructor(val prefs: FetchBuilderPrefs) {

        val uiHandler = Handler(Looper.getMainLooper())
        val handler: Handler
        val fetchListenerProvider: ListenerProvider
        val downloadManager: DownloadManager
        val databaseManager: DatabaseManager
        val downloadInfoManagerDelegate: DownloadInfoManagerDelegate
        val priorityQueueProcessor: PriorityQueueProcessor<Download>
        val fetchHandler: FetchHandler

        init {
            val handlerThread = HandlerThread("fetch_${prefs.namespace}")
            handlerThread.start()
            handler = Handler(handlerThread.looper)

            fetchListenerProvider = ListenerProvider()

            databaseManager = DatabaseManagerImpl(
                    context = prefs.appContext,
                    namespace = prefs.namespace,
                    isMemoryDatabase = prefs.inMemoryDatabaseEnabled,
                    logger = prefs.logger)

            downloadManager = DownloadManagerImpl(
                    downloader = defaultDownloader,
                    concurrentLimit = prefs.concurrentLimit,
                    progressReportingIntervalMillis = prefs.progressReportingIntervalMillis,
                    downloadBufferSizeBytes = prefs.downloadBufferSizeBytes,
                    logger = prefs.logger)

            downloadInfoManagerDelegate = DownloadInfoManagerDelegate(
                    downloadInfoUpdater = DownloadInfoUpdater(databaseManager),
                    uiHandler = uiHandler,
                    fetchListener = fetchListenerProvider.mainListener,
                    logger = prefs.logger)

            downloadManager.delegate = downloadInfoManagerDelegate

            priorityQueueProcessor = PriorityQueueProcessorImpl(
                    handler = handler,
                    downloadProvider = DownloadProvider(databaseManager),
                    downloadManager = downloadManager,
                    networkProvider = NetworkProvider(prefs.appContext),
                    logger = prefs.logger)

            priorityQueueProcessor.globalNetworkType = prefs.globalNetworkType

            fetchHandler = FetchHandlerImpl(
                    namespace = prefs.namespace,
                    databaseManager = databaseManager,
                    downloadManager = downloadManager,
                    priorityQueueProcessor = priorityQueueProcessor,
                    fetchListenerProvider = fetchListenerProvider,
                    handler = handler,
                    logger = prefs.logger)
        }

    }

}