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
import com.tonyodev.fetch2.helper.DownloadInfoUpdater
import com.tonyodev.fetch2.helper.PriorityListProcessor
import com.tonyodev.fetch2.helper.PriorityListProcessorImpl
import com.tonyodev.fetch2.provider.DownloadProvider
import com.tonyodev.fetch2.provider.ListenerProvider
import com.tonyodev.fetch2.provider.NetworkInfoProvider
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
        val priorityListProcessor: PriorityListProcessor<Download>
        val fetchHandler: FetchHandler
        val networkInfoProvider: NetworkInfoProvider
        val downloadProvider: DownloadProvider
        val downloadInfoUpdater: DownloadInfoUpdater
        val migrations = DownloadDatabase.getMigrations()

        init {
            val handlerThread = HandlerThread("fetch_${prefs.namespace}")
            handlerThread.start()
            handler = Handler(handlerThread.looper)

            fetchListenerProvider = ListenerProvider()
            networkInfoProvider = NetworkInfoProvider(prefs.appContext)

            databaseManager = DatabaseManagerImpl(
                    context = prefs.appContext,
                    namespace = prefs.namespace,
                    isMemoryDatabase = prefs.inMemoryDatabaseEnabled,
                    logger = prefs.logger,
                    migrations = migrations)

            downloadProvider = DownloadProvider(databaseManager)
            downloadInfoUpdater = DownloadInfoUpdater(databaseManager)

            downloadManager = DownloadManagerImpl(
                    downloader = prefs.downloader,
                    concurrentLimit = prefs.concurrentLimit,
                    progressReportingIntervalMillis = prefs.progressReportingIntervalMillis,
                    downloadBufferSizeBytes = prefs.downloadBufferSizeBytes,
                    logger = prefs.logger,
                    networkInfoProvider = networkInfoProvider,
                    retryOnNetworkGain = prefs.retryOnNetworkGain,
                    fetchListenerProvider = fetchListenerProvider,
                    uiHandler = uiHandler,
                    downloadInfoUpdater = downloadInfoUpdater)

            priorityListProcessor = PriorityListProcessorImpl(
                    handler = handler,
                    downloadProvider = downloadProvider,
                    downloadManager = downloadManager,
                    networkInfoProvider = networkInfoProvider,
                    logger = prefs.logger)

            priorityListProcessor.globalNetworkType = prefs.globalNetworkType

            fetchHandler = FetchHandlerImpl(
                    namespace = prefs.namespace,
                    databaseManager = databaseManager,
                    downloadManager = downloadManager,
                    priorityListProcessor = priorityListProcessor,
                    fetchListenerProvider = fetchListenerProvider,
                    handler = handler,
                    logger = prefs.logger,
                    autoStart = prefs.autoStart)
        }

    }

}