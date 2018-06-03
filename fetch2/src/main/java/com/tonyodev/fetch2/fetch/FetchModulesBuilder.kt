package com.tonyodev.fetch2.fetch

import android.os.Handler
import android.os.Looper
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.database.DatabaseManager
import com.tonyodev.fetch2.database.DatabaseManagerImpl
import com.tonyodev.fetch2.database.DownloadDatabase
import com.tonyodev.fetch2.downloader.DownloadManager
import com.tonyodev.fetch2.downloader.DownloadManagerImpl
import com.tonyodev.fetch2.helper.DownloadInfoUpdater
import com.tonyodev.fetch2.helper.PriorityListProcessor
import com.tonyodev.fetch2.helper.PriorityListProcessorImpl
import com.tonyodev.fetch2.provider.DownloadProvider
import com.tonyodev.fetch2.provider.NetworkInfoProvider
import com.tonyodev.fetch2.util.getFileTempDir

object FetchModulesBuilder {

    private val lock = Any()
    private val holderMap = mutableMapOf<String, Holder>()

    fun buildModulesFromPrefs(fetchConfiguration: FetchConfiguration): Modules {
        return synchronized(lock) {
            val holder = holderMap[fetchConfiguration.namespace]
            val modules = if (holder != null) {
                Modules(fetchConfiguration, holder.handlerWrapper, holder.databaseManager)
            } else {
                val newHandlerWrapper = HandlerWrapper(fetchConfiguration.namespace)
                val newDatabaseManager = DatabaseManagerImpl(
                        context = fetchConfiguration.appContext,
                        namespace = fetchConfiguration.namespace,
                        logger = fetchConfiguration.logger,
                        migrations = DownloadDatabase.getMigrations())
                val newModules = Modules(fetchConfiguration, newHandlerWrapper, newDatabaseManager)
                holderMap[fetchConfiguration.namespace] = Holder(newHandlerWrapper, newDatabaseManager)
                newModules
            }
            modules.handlerWrapper.incrementUsageCounter()
            modules
        }
    }

    fun removeNamespaceInstanceReference(namespace: String) {
        synchronized(lock) {
            val holder = holderMap[namespace]
            if (holder != null) {
                holder.handlerWrapper.decrementUsageCounter()
                if (holder.handlerWrapper.usageCount() == 0) {
                    holder.handlerWrapper.close()
                    holder.databaseManager.close()
                    holderMap.remove(namespace)
                }
            }
        }
    }

    data class Holder(val handlerWrapper: HandlerWrapper, val databaseManager: DatabaseManager)

    class Modules constructor(val fetchConfiguration: FetchConfiguration,
                              val handlerWrapper: HandlerWrapper,
                              databaseManager: DatabaseManager) {

        private val downloadManager: DownloadManager
        private val priorityListProcessor: PriorityListProcessor<Download>
        private val downloadProvider = DownloadProvider(databaseManager)
        private val downloadInfoUpdater = DownloadInfoUpdater(databaseManager)
        private val networkInfoProvider = NetworkInfoProvider(fetchConfiguration.appContext)
        val fetchHandler: FetchHandler
        val uiHandler = Handler(Looper.getMainLooper())

        init {
            downloadManager = DownloadManagerImpl(
                    downloader = fetchConfiguration.downloader,
                    concurrentLimit = fetchConfiguration.concurrentLimit,
                    progressReportingIntervalMillis = fetchConfiguration.progressReportingIntervalMillis,
                    downloadBufferSizeBytes = fetchConfiguration.downloadBufferSizeBytes,
                    logger = fetchConfiguration.logger,
                    networkInfoProvider = networkInfoProvider,
                    retryOnNetworkGain = fetchConfiguration.retryOnNetworkGain,
                    uiHandler = uiHandler,
                    downloadInfoUpdater = downloadInfoUpdater,
                    fileTempDir = getFileTempDir(fetchConfiguration.appContext),
                    namespace = fetchConfiguration.namespace)
            priorityListProcessor = PriorityListProcessorImpl(
                    handlerWrapper = handlerWrapper,
                    downloadProvider = downloadProvider,
                    downloadManager = downloadManager,
                    networkInfoProvider = networkInfoProvider,
                    logger = fetchConfiguration.logger)
            priorityListProcessor.globalNetworkType = fetchConfiguration.globalNetworkType
            fetchHandler = FetchHandlerImpl(
                    namespace = fetchConfiguration.namespace,
                    databaseManager = databaseManager,
                    downloadManager = downloadManager,
                    priorityListProcessor = priorityListProcessor,
                    logger = fetchConfiguration.logger,
                    autoStart = fetchConfiguration.autoStart,
                    downloader = fetchConfiguration.downloader,
                    fileTempDir = getFileTempDir(fetchConfiguration.appContext))
        }

    }

}