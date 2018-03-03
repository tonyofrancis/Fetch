package com.tonyodev.fetch2.fetch

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.database.DatabaseManager
import com.tonyodev.fetch2.database.DatabaseManagerImpl
import com.tonyodev.fetch2.database.DownloadDatabase
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2.downloader.DownloadManagerImpl
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.helper.*
import com.tonyodev.fetch2.provider.DownloadProvider
import com.tonyodev.fetch2.provider.ListenerProvider
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.provider.NetworkInfoProviderImpl
import com.tonyodev.fetch2.util.FETCH_ALREADY_EXIST

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

    class Modules constructor(val prefs: FetchBuilderPrefs) {

        val uiHandler = Handler(Looper.getMainLooper())
        val handler: Handler
        val fetchListenerProvider: ListenerProvider
        val downloadManager: DownloadManager
        val databaseManager: DatabaseManager
        val downloadManagerDelegateImpl: DownloadManagerDelegateImpl
        val priorityIteratorProcessor: PriorityIteratorProcessor<Download>
        val priorityIteratorProcessorHandler: PriorityIteratorProcessorHandler
        val networkInfoProvider: NetworkInfoProvider
        val fetchHandler: FetchHandler

        init {
            val handlerThread = HandlerThread("fetch_${prefs.namespace}")
            handlerThread.start()
            handler = Handler(handlerThread.looper)

            fetchListenerProvider = ListenerProvider()

            networkInfoProvider = NetworkInfoProviderImpl(context = prefs.appContext,
                    logger = prefs.logger)

            databaseManager = DatabaseManagerImpl(
                    context = prefs.appContext,
                    namespace = prefs.namespace,
                    isMemoryDatabase = prefs.inMemoryDatabaseEnabled,
                    logger = prefs.logger,
                    migrations = DownloadDatabase.getMigrations())

            downloadManager = DownloadManagerImpl(
                    downloader = prefs.downloader,
                    concurrentLimit = prefs.concurrentLimit,
                    progressReportingIntervalMillis = prefs.progressReportingIntervalMillis,
                    downloadBufferSizeBytes = prefs.downloadBufferSizeBytes,
                    logger = prefs.logger,
                    networkInfoProvider = networkInfoProvider)

            priorityIteratorProcessor = PriorityIteratorProcessorImpl(
                    handler = handler,
                    downloadProvider = DownloadProvider(databaseManager),
                    downloadManager = downloadManager,
                    networkInfoProvider = networkInfoProvider,
                    logger = prefs.logger)

            priorityIteratorProcessorHandler = PriorityIteratorProcessorHandlerImpl(
                    handler = handler,
                    priorityIteratorProcessor = priorityIteratorProcessor,
                    logger = prefs.logger)

            downloadManagerDelegateImpl = DownloadManagerDelegateImpl(
                    downloadInfoUpdater = DownloadInfoUpdater(databaseManager),
                    uiHandler = uiHandler,
                    fetchListener = fetchListenerProvider.mainListener,
                    logger = prefs.logger,
                    priorityIteratorProcessorHandler = priorityIteratorProcessorHandler,
                    retryOnConnectionGain = prefs.retryOnConnectionGain)

            downloadManager.delegate = downloadManagerDelegateImpl

            priorityIteratorProcessor.globalNetworkType = prefs.globalNetworkType

            fetchHandler = FetchHandlerImpl(
                    namespace = prefs.namespace,
                    databaseManager = databaseManager,
                    downloadManager = downloadManager,
                    priorityIteratorProcessor = priorityIteratorProcessor,
                    fetchListenerProvider = fetchListenerProvider,
                    handler = handler,
                    logger = prefs.logger,
                    autoStartProcessing = prefs.autoStartProcessing,
                    retryOnConnectionGain = prefs.retryOnConnectionGain,
                    networkInfoProvider = networkInfoProvider)
        }

    }

}