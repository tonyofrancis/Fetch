package com.tonyodev.fetch2.fetch

import android.content.Context
import com.tonyodev.fetch2.Downloader
import com.tonyodev.fetch2.FetchLogger
import com.tonyodev.fetch2.Logger
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.util.*

/**
 * Class used to generate an instance of Fetch with configurations.
 * */
abstract class FetchBuilder<out B, out F> constructor(
        /** Context*/
        context: Context,

        /** The namespace which Fetch operates in. Fetch uses
         * a namespace to create a database that the instance will use. Downloads
         * enqueued on the instance will belong to the namespace and will not be accessible
         * from any other namespace. An App can only have one Active Fetch instance with the
         * specified namespace. * In essence an App can have many instances of fetch
         * with a different namespaces.
         * */
        private val namespace: String) {

    private val appContext = context.applicationContext
    private var concurrentLimit = DEFAULT_CONCURRENT_LIMIT
    private var progressReportingIntervalMillis = DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS
    private var downloadBufferSizeBytes = DEFAULT_DOWNLOAD_BUFFER_SIZE_BYTES
    private var loggingEnabled = DEFAULT_LOGGING_ENABLED
    private var inMemoryDatabaseEnabled = DEFAULT_IN_MEMORY_DATABASE_ENABLED
    private var downloader = defaultDownloader
    private var globalNetworkType = defaultGlobalNetworkType
    private var logger: Logger = defaultLogger
    private var autoStart = DEFAULT_AUTO_START
    private var retryOnNetworkGain = DEFAULT_RETRY_ON_NETWORK_GAIN

    /**
     * Sets the downloader client Fetch will use to perform downloads.
     * The default downloader uses the HttpUrlConnection client to perform downloads.
     * @see com.tonyodev.fetch2.Downloader
     * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader
     * @see com.tonyodev.fetch2downloader.OkHttpDownloader
     * @param downloader Downloader Client
     * @return com.tonyodev.fetch2.Fetch.Builder.this
     * */
    fun setDownloader(downloader: Downloader): FetchBuilder<B, F> {
        this.downloader = downloader
        return this
    }

    /**
     * Sets the progress reporting interval in milliseconds. This controls how often
     * @see com.tonyodev.fetch2.FetchListener.onProgress is called for each
     * download. The default value is 2 seconds.
     * This method can only accept values greater than 0.
     * @param progressReportingIntervalMillis Progress reporting interval in milliseconds
     * @throws FetchException if the passed in progress reporting interval is less than 0.
     * @return com.tonyodev.fetch2.Fetch.Builder.this
     * */
    fun setProgressReportingInterval(progressReportingIntervalMillis: Long): FetchBuilder<B, F> {
        if (progressReportingIntervalMillis < 0) {
            throw FetchException("progressReportingIntervalMillis cannot be less than 0",
                    FetchException.Code.ILLEGAL_ARGUMENT)
        }
        this.progressReportingIntervalMillis = progressReportingIntervalMillis
        return this
    }

    /** Sets the number of parallel downloads Fetch should perform at any given time.
     * Default value is 1. This method can only accept values greater than 0.
     * @param downloadConcurrentLimit Number of parallel downloads.
     * @throws FetchException if the passed in download concurrent limit is less than 1.
     * @return com.tonyodev.fetch2.Fetch.Builder.this
     * */
    fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int): FetchBuilder<B, F> {
        if (downloadConcurrentLimit < 1) {
            throw FetchException("Concurrent limit cannot be less " +
                    "than 1",
                    FetchException.Code.ILLEGAL_ARGUMENT)
        }
        this.concurrentLimit = downloadConcurrentLimit
        return this
    }

    /**
     * Overrides each downloads specified network type preference and use the
     * global network type preference instead. The default is GLOBAL_OFF.
     * @see com.tonyodev.fetch2.NetworkType
     * @param networkType The global network type.
     * @return com.tonyodev.fetch2.Fetch.Builder.this
     * */
    fun setGlobalNetworkType(networkType: NetworkType): FetchBuilder<B, F> {
        this.globalNetworkType = networkType
        return this
    }

    /**
     * Enable or disable logging.
     * Default is false.
     * @param enabled Enable or disable logging.
     * @return com.tonyodev.fetch2.Fetch.Builder.this
     * */
    fun enableLogging(enabled: Boolean): FetchBuilder<B, F> {
        this.loggingEnabled = enabled
        return this
    }

    /**
     * Configures Fetch to store and maintain download information in memory.
     * Download Request information will not be persisted to disk and will be discarded when
     * the Fetch instance is released. However, Files downloaded to the disk are persisted.
     * Default is false.
     * @param enabled Enable in memory database.
     * @return com.tonyodev.fetch2.Fetch.Builder.this
     * */
    fun enabledInMemoryDatabase(enabled: Boolean): FetchBuilder<B, F> {
        this.inMemoryDatabaseEnabled = enabled
        return this
    }

    /** Sets the buffer size for downloads. Default is 8192 bytes.
     * @param bytes buffer size. Has to be greater than 0.
     * @throws FetchException if the passed in buffer size is less than 1.
     * @return com.tonyodev.fetch2.Fetch.Builder.this
     * */
    fun setDownloadBufferSize(bytes: Int): FetchBuilder<B, F> {
        if (bytes < 1) {
            throw FetchException("Buffer size cannot be less than 1.",
                    FetchException.Code.ILLEGAL_ARGUMENT)
        }
        this.downloadBufferSizeBytes = bytes
        return this
    }

    /** Sets custom logger.
     * @param logger custom logger.
     * */
    fun setLogger(logger: Logger): FetchBuilder<B, F> {
        this.logger = logger
        return this
    }

    /** Allows Fetch to start processing old requests after the build method is called
     * on the builder. Default is true.
     * @param enabled enable or disable auto start.
     * @return com.tonyodev.fetch2.Fetch.Builder.this
     * */
    fun enableAutoStart(enabled: Boolean): FetchBuilder<B, F> {
        this.autoStart = enabled
        return this
    }

    /** Allows Fetch to auto try downloading a request if the network connection was lost when the request
     * was being downloaded. The download will automatically resume when network connection is gained.
     * Default is false.
     * @param enabled enable or disable
     * @return com.tonyodev.fetch2.Fetch.Builder.this
     * */
    fun enableRetryOnNetworkGain(enabled: Boolean): FetchBuilder<B, F> {
        this.retryOnNetworkGain = enabled
        return this
    }

    /** Gets this builders current configuration settings.
     * @return Builder configuration settings.
     * */
    fun getBuilderPrefs(): FetchBuilderPrefs {
        val prefsLogger = logger
        if (prefsLogger is FetchLogger) {
            prefsLogger.enabled = loggingEnabled
            prefsLogger.tag = namespace
        } else {
            logger.enabled = loggingEnabled
        }
        return FetchBuilderPrefs(
                appContext = appContext,
                namespace = namespace,
                concurrentLimit = concurrentLimit,
                progressReportingIntervalMillis = progressReportingIntervalMillis,
                downloadBufferSizeBytes = downloadBufferSizeBytes,
                loggingEnabled = loggingEnabled,
                inMemoryDatabaseEnabled = inMemoryDatabaseEnabled,
                downloader = downloader,
                globalNetworkType = globalNetworkType,
                logger = prefsLogger,
                autoStart = autoStart,
                retryOnNetworkGain = retryOnNetworkGain)
    }

    /** Builds a new instance of Fetch with the proper configuration.
     * @throws FetchException if an active instance of Fetch with the same namespace already
     * exists.
     * @return New instance of Fetch.
     * */
    abstract fun build(): F

}