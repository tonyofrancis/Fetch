package com.tonyodev.fetch2

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2.database.FetchDatabaseManager
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.util.*
import com.tonyodev.fetch2core.*

/**
 * Class used to hold the configuration settings for a Fetch instance.
 * @see Builder
 * */
class FetchConfiguration private constructor(val appContext: Context,
                                             val namespace: String,
                                             val concurrentLimit: Int,
                                             val progressReportingIntervalMillis: Long,
                                             val loggingEnabled: Boolean,
                                             val httpDownloader: Downloader<*, *>,
                                             val globalNetworkType: NetworkType,
                                             val logger: Logger,
                                             val autoStart: Boolean,
                                             val retryOnNetworkGain: Boolean,
                                             val fileServerDownloader: FileServerDownloader,
                                             val hashCheckingEnabled: Boolean,
                                             val fileExistChecksEnabled: Boolean,
                                             val storageResolver: StorageResolver,
                                             val fetchNotificationManager: FetchNotificationManager?,
                                             val fetchDatabaseManager: FetchDatabaseManager<DownloadInfo>?,
                                             val backgroundHandler: Handler?,
                                             val prioritySort: PrioritySort,
                                             val internetCheckUrl: String?,
                                             val activeDownloadsCheckInterval: Long,
                                             val createFileOnEnqueue: Boolean,
                                             val maxAutoRetryAttempts: Int,
                                             val preAllocateFileOnCreation: Boolean) {

    /* Creates a new Instance of Fetch with this object's configuration settings. Convenience method
    * for Fetch.Impl.getInstance(fetchConfiguration)
    * @return new Fetch instance
    * */
    fun getNewFetchInstanceFromConfiguration(): Fetch {
        return Fetch.getInstance(this)
    }

    /** Used to create an instance of Fetch Configuration.*/
    class Builder(context: Context) {

        private val appContext = context.applicationContext
        private var namespace = DEFAULT_INSTANCE_NAMESPACE
        private var concurrentLimit = DEFAULT_CONCURRENT_LIMIT
        private var progressReportingIntervalMillis = DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS
        private var loggingEnabled = DEFAULT_LOGGING_ENABLED
        private var httpDownloader = defaultDownloader
        private var globalNetworkType = defaultGlobalNetworkType
        private var logger: Logger = defaultLogger
        private var autoStart = DEFAULT_AUTO_START
        private var retryOnNetworkGain = DEFAULT_RETRY_ON_NETWORK_GAIN
        private var fileServerDownloader: FileServerDownloader = defaultFileServerDownloader
        private var hashCheckEnabled = DEFAULT_HASH_CHECK_ENABLED
        private var fileExistChecksEnabled = DEFAULT_FILE_EXIST_CHECKS
        private var storageResolver: StorageResolver = DefaultStorageResolver(appContext, getFileTempDir(appContext))
        private var fetchNotificationManager: FetchNotificationManager? = null
        private var fetchDatabaseManager: FetchDatabaseManager<DownloadInfo>? = null
        private var backgroundHandler: Handler? = null
        private var prioritySort: PrioritySort = defaultPrioritySort
        private var internetCheckUrl: String? = null
        private var activeDownloadCheckInterval = DEFAULT_HAS_ACTIVE_DOWNLOADS_INTERVAL_IN_MILLISECONDS
        private var createFileOnEnqueue = DEFAULT_CREATE_FILE_ON_ENQUEUE
        private var maxAutoRetryAttempts = DEFAULT_GLOBAL_AUTO_RETRY_ATTEMPTS
        private var preAllocateFileOnCreation = DEFAULT_PREALLOCATE_FILE_ON_CREATE

        /** Sets the namespace which Fetch operates in. Fetch uses
         * a namespace to create a database that the instance will use. Downloads
         * enqueued on the Fetch instance will belong to the namespace and will not be accessible
         * from any other namespace.
         * @param namespace name. If null or empty, the Global namespace is used.
         * @return Builder
         * */
        fun setNamespace(namespace: String? = null): Builder {
            this.namespace = if (namespace == null || namespace.isEmpty()) {
                DEFAULT_INSTANCE_NAMESPACE
            } else {
                namespace
            }
            return this
        }

        /**
         * Sets the httpDownloader client Fetch will use to perform http downloads.
         * The default httpDownloader uses the HttpUrlConnection client to perform downloads.
         * @see com.tonyodev.fetch2core.Downloader
         * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader
         * @see com.tonyodev.fetch2okhttp.OkHttpDownloader
         * @param downloader Downloader Client
         * @return Builder
         * */
        fun setHttpDownloader(downloader: Downloader<*, *>): Builder {
            this.httpDownloader = downloader
            return this
        }

        /**
         * Sets the downloader client Fetch will use to perform downloads from a TCP File Server.
         * @see com.tonyodev.fetch2.Downloader
         * @see com.tonyodev.fetch2.FileServerDownloader
         * @param downloader Downloader Client for Fetch File Server
         * @return Builder
         * */
        fun setFileServerDownloader(fileServerDownloader: FileServerDownloader): Builder {
            this.fileServerDownloader = fileServerDownloader
            return this
        }

        /**
         * Sets the progress reporting interval in milliseconds. This controls how often
         * @see com.tonyodev.fetch2.FetchListener.onProgress is called for each
         * download. The default value is 2 seconds.
         * This method can only accept values greater than 0.
         * @param progressReportingIntervalMillis Progress reporting interval in milliseconds
         * @throws FetchException if the passed in progress reporting interval is less than 0.
         * @return Builder
         * */
        fun setProgressReportingInterval(progressReportingIntervalMillis: Long): Builder {
            if (progressReportingIntervalMillis < 0) {
                throw FetchException("progressReportingIntervalMillis cannot be less than 0")
            }
            this.progressReportingIntervalMillis = progressReportingIntervalMillis
            return this
        }

        /** Sets the number of parallel downloads Fetch should perform at any given time.
         * Default value is 1. This method can only accept values greater than -1. Setting the
         * concurrent limit to zero prevents the instance of Fetch to pull and download request
         * from the waiting queue but allows the instance of Fetch to act on and observe changes to
         * requests/downloads.
         * @param downloadConcurrentLimit Number of parallel downloads.
         * @throws FetchException if the passed in download concurrent limit is less than 0.
         * @return Builder
         * */
        fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int): Builder {
            if (downloadConcurrentLimit < 0) {
                throw FetchException("Concurrent limit cannot be less than 0")
            }
            this.concurrentLimit = downloadConcurrentLimit
            return this
        }

        /**
         * Overrides each downloads specified network type preference and use the
         * global network type preference instead. The default is GLOBAL_OFF.
         * @see com.tonyodev.fetch2.NetworkType
         * @param networkType The global network type.
         * @return Builder
         * */
        fun setGlobalNetworkType(networkType: NetworkType): Builder {
            this.globalNetworkType = networkType
            return this
        }

        /**
         * Enable or disable logging.
         * Default is false.
         * @param enabled Enable or disable logging.
         * @return Builder
         * */
        fun enableLogging(enabled: Boolean): Builder {
            this.loggingEnabled = enabled
            return this
        }

        /** Sets custom logger.
         * @param logger custom logger.
         * @return Builder
         * */
        fun setLogger(logger: Logger): Builder {
            this.logger = logger
            return this
        }

        /** Allows Fetch to start processing old requests after the build method is called
         * on the builder. Default is true.
         * @param enabled enable or disable auto start.
         * @return Builder
         * */
        fun enableAutoStart(enabled: Boolean): Builder {
            this.autoStart = enabled
            return this
        }

        /** Allows Fetch to auto try downloading a request if the network connection was lost when the request
         * was being downloaded. The download will automatically resume when network connection is gained.
         * Default is false.
         * @param enabled enable or disable
         * @return Builder
         * */
        fun enableRetryOnNetworkGain(enabled: Boolean): Builder {
            this.retryOnNetworkGain = enabled
            return this
        }

        /** Allows Fetch to check the fully downloaded file hash checksum after the download completes
         * with hash checksum returned by the server if supported. This check is only ran once.
         * Default is false
         * @param enabled hash checking enabled
         * @return Builder
         * */
        fun enableHashCheck(enabled: Boolean): Builder {
            this.hashCheckEnabled = enabled
            return this
        }

        /**
         * Allows Fetch to check if the file exist for a download. If the file does not exist,
         * Fetch will update the database and queued, paused or downloading downloads will
         * have to start at the beginning. Enabled by default. Set to false only if you are
         * monitoring the file existence elsewhere.
         * @param enable set if file checking is enabled
         * @return Builder
         * */
        fun enableFileExistChecks(enabled: Boolean): Builder {
            this.fileExistChecksEnabled = enabled
            return this
        }

        /**
         * Set the storage Resolver used by Fetch. See Java docs for StorageResolver interface
         * to see its use. If not set, The default storage resolver is used.
         * @param storageResolver the storage resolver
         * @return Builder
         * */
        fun setStorageResolver(storageResolver: StorageResolver): Builder {
            this.storageResolver = storageResolver
            return this
        }

        /** Set the notification manager used by instances of Fetch created by this configuration.
         * Notifications are not enabled by default.
         * See Java docs for FetchNotificationManager interface or DefaultNotificationManager class
         * to see its use.
         * @param fetchNotificationManager the notification manager. If null, notifications
         * are not enabled on the Fetch instances created by this configuration.
         * @return Builder
         * */
        fun setNotificationManager(fetchNotificationManager: FetchNotificationManager?): Builder {
            this.fetchNotificationManager = fetchNotificationManager
            return this
        }

        /**
         * Set the database manager used by instances of Fetch created by this configuration.
         * See Java docs for FetchDatabaseManager interface or FetchDatabaseManagerImpl class on
         * default use.
         * @param fetchDatabaseManager the Fetch database managers.
         * @return Builder
         * */
        fun setDatabaseManager(fetchDatabaseManager: FetchDatabaseManager<DownloadInfo>?): Builder {
            this.fetchDatabaseManager = fetchDatabaseManager
            return this
        }

        /**
         * Sets the handler(worker thread) Fetch namespace uses to perform all operations on.
         * This handler cannot contain the main thread. Any instance of Fetch that uses the
         * passed in namespace in this configuration will use this handler.
         * @param handler the handler.
         * @throws IllegalAccessException if handler uses main thread.
         * @return Builder
         * */
        fun setBackgroundHandler(handler: Handler): Builder {
            if (handler.looper.thread == Looper.getMainLooper().thread) {
                throw IllegalAccessException("The background handler cannot use the main/ui thread")
            }
            this.backgroundHandler = handler
            return this
        }

        /**
         * Used to dictate the order in which Fetch processes request/downloads
         * based on time created. Default is PrioritySort.ASC
         * @param prioritySort the priority sort.
         * */
        fun setPrioritySort(prioritySort: PrioritySort): Builder {
            this.prioritySort = prioritySort
            return this
        }

        /**
         * If set, Fetch will use this url to check against for internet connection.
         * rather than using the Android frameworks internet check classes. Note: setting
         * this will cause Fetch to always make a network request to the url.
         * This will cause a small delay before downloading a request. Only set this option if
         * necessary.
         * @param url the url to check against.
         * */
        fun setInternetAccessUrlCheck(url: String?): Builder {
            internetCheckUrl = url
            return this
        }

        /**
         * Sets the HasActiveDownloads reporting interval in milliseconds for instances of Fetch
         * created with this FetchConfiguration. This controls how often HasActiveDownloads is reported
         * when a FetchObserver is attached to Fetch to monitor hasActiveDownloads.
         * The default value is 5 minutes.
         * This method can only accept values greater than 0.
         * @param intervalInMillis reporting interval in milliseconds
         * @throws FetchException if the passed in progress reporting interval is less than 0.
         * @return Builder
         * */
        fun setHasActiveDownloadsCheckInterval(intervalInMillis: Long): Builder {
            if (intervalInMillis < 0) {
                throw FetchException("intervalInMillis cannot be less than 0")
            }
            this.activeDownloadCheckInterval = intervalInMillis
            return this
        }

        /**
         * Enable or disable creating the download file on enqueue
         * @param create true or false. The default is true
         * @return Builder
         * */
        fun createDownloadFileOnEnqueue(create: Boolean): Builder {
            this.createFileOnEnqueue = create
            return this
        }
        /**
         * The Global maximum number of times Fetch will auto retry a failed download. If set,
         * the autoRetryMaxAttempts on the individual download is overridden.
         * @throws IllegalArgumentException if value passed in is less than 0
         * */
        fun setAutoRetryMaxAttempts(autoRetryMaxAttempts: Int): Builder {
            if (autoRetryMaxAttempts < 0) {
                throw IllegalArgumentException("The AutoRetryMaxAttempts has to be greater than -1")
            }
            this.maxAutoRetryAttempts = autoRetryMaxAttempts
            return this
        }

        /**
         * Preallocate the download file on the local storage when a request is enqueued.
         * @param preAllocateFile true to pre allocate. False otherwise. The default is true.
         * @return Builder
         * */
        fun preAllocateFileOnCreation(preAllocateFile: Boolean): Builder {
            this.preAllocateFileOnCreation = preAllocateFile
            return this
        }

        /**
         * Build FetchConfiguration instance.
         * @return new FetchConfiguration instance.
         * */
        fun build(): FetchConfiguration {
            val prefsLogger = logger
            if (prefsLogger is FetchLogger) {
                prefsLogger.enabled = loggingEnabled
                if (prefsLogger.tag == DEFAULT_TAG) {
                    prefsLogger.tag = namespace
                }
            } else {
                logger.enabled = loggingEnabled
            }
            return FetchConfiguration(
                    appContext = appContext,
                    namespace = namespace,
                    concurrentLimit = concurrentLimit,
                    progressReportingIntervalMillis = progressReportingIntervalMillis,
                    loggingEnabled = loggingEnabled,
                    httpDownloader = httpDownloader,
                    globalNetworkType = globalNetworkType,
                    logger = prefsLogger,
                    autoStart = autoStart,
                    retryOnNetworkGain = retryOnNetworkGain,
                    fileServerDownloader = fileServerDownloader,
                    hashCheckingEnabled = hashCheckEnabled,
                    fileExistChecksEnabled = fileExistChecksEnabled,
                    storageResolver = storageResolver,
                    fetchNotificationManager = fetchNotificationManager,
                    fetchDatabaseManager = fetchDatabaseManager,
                    backgroundHandler = backgroundHandler,
                    prioritySort = prioritySort,
                    internetCheckUrl = internetCheckUrl,
                    activeDownloadsCheckInterval = activeDownloadCheckInterval,
                    createFileOnEnqueue = createFileOnEnqueue,
                    maxAutoRetryAttempts = maxAutoRetryAttempts,
                    preAllocateFileOnCreation = preAllocateFileOnCreation)
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FetchConfiguration
        if (appContext != other.appContext) return false
        if (namespace != other.namespace) return false
        if (concurrentLimit != other.concurrentLimit) return false
        if (progressReportingIntervalMillis != other.progressReportingIntervalMillis) return false
        if (loggingEnabled != other.loggingEnabled) return false
        if (httpDownloader != other.httpDownloader) return false
        if (globalNetworkType != other.globalNetworkType) return false
        if (logger != other.logger) return false
        if (autoStart != other.autoStart) return false
        if (retryOnNetworkGain != other.retryOnNetworkGain) return false
        if (fileServerDownloader != other.fileServerDownloader) return false
        if (hashCheckingEnabled != other.hashCheckingEnabled) return false
        if (fileExistChecksEnabled != other.fileExistChecksEnabled) return false
        if (storageResolver != other.storageResolver) return false
        if (fetchNotificationManager != other.fetchNotificationManager) return false
        if (fetchDatabaseManager != other.fetchDatabaseManager) return false
        if (backgroundHandler != other.backgroundHandler) return false
        if (prioritySort != other.prioritySort) return false
        if (internetCheckUrl != other.internetCheckUrl) return false
        if (activeDownloadsCheckInterval != other.activeDownloadsCheckInterval) return false
        if (createFileOnEnqueue != other.createFileOnEnqueue) return false
        if (maxAutoRetryAttempts != other.maxAutoRetryAttempts) return false
        if (preAllocateFileOnCreation != other.preAllocateFileOnCreation) return false
        return true
    }

    override fun hashCode(): Int {
        var result = appContext.hashCode()
        result = 31 * result + namespace.hashCode()
        result = 31 * result + concurrentLimit
        result = 31 * result + progressReportingIntervalMillis.hashCode()
        result = 31 * result + loggingEnabled.hashCode()
        result = 31 * result + httpDownloader.hashCode()
        result = 31 * result + globalNetworkType.hashCode()
        result = 31 * result + logger.hashCode()
        result = 31 * result + autoStart.hashCode()
        result = 31 * result + retryOnNetworkGain.hashCode()
        result = 31 * result + fileServerDownloader.hashCode()
        result = 31 * result + hashCheckingEnabled.hashCode()
        result = 31 * result + fileExistChecksEnabled.hashCode()
        result = 31 * result + storageResolver.hashCode()
        if (fetchNotificationManager != null) {
            result = 31 * result + fetchNotificationManager.hashCode()
        }
        if (fetchDatabaseManager != null) {
            result = 31 * result + fetchDatabaseManager.hashCode()
        }
        if (backgroundHandler != null) {
            result = 31 * result + backgroundHandler.hashCode()
        }
        result = 31 * result + prioritySort.hashCode()
        if (internetCheckUrl != null) {
            result = 31 * result + internetCheckUrl.hashCode()
        }
        result = 31 * result + activeDownloadsCheckInterval.hashCode()
        result = 31 * result + createFileOnEnqueue.hashCode()
        result = 31 * result + maxAutoRetryAttempts.hashCode()
        result = 31 * result + preAllocateFileOnCreation.hashCode()
        return result
    }

    override fun toString(): String {
        return "FetchConfiguration(appContext=$appContext, namespace='$namespace', " +
                "concurrentLimit=$concurrentLimit, progressReportingIntervalMillis=$progressReportingIntervalMillis, " +
                "loggingEnabled=$loggingEnabled, httpDownloader=$httpDownloader, globalNetworkType=$globalNetworkType," +
                " logger=$logger, autoStart=$autoStart, retryOnNetworkGain=$retryOnNetworkGain, " +
                "fileServerDownloader=$fileServerDownloader, hashCheckingEnabled=$hashCheckingEnabled, " +
                "fileExistChecksEnabled=$fileExistChecksEnabled, storageResolver=$storageResolver, " +
                "fetchNotificationManager=$fetchNotificationManager, fetchDatabaseManager=$fetchDatabaseManager," +
                " backgroundHandler=$backgroundHandler, prioritySort=$prioritySort, internetCheckUrl=$internetCheckUrl," +
                " activeDownloadsCheckInterval=$activeDownloadsCheckInterval, createFileOnEnqueue=$createFileOnEnqueue," +
                " preAllocateFileOnCreation=$preAllocateFileOnCreation, " +
                "maxAutoRetryAttempts=$maxAutoRetryAttempts)"
    }

}