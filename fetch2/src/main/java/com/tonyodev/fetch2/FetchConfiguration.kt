package com.tonyodev.fetch2

import android.content.Context
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
                                             val httpDownloader: Downloader,
                                             val globalNetworkType: NetworkType,
                                             val logger: Logger,
                                             val autoStart: Boolean,
                                             val retryOnNetworkGain: Boolean,
                                             val fileServerDownloader: FileServerDownloader,
                                             val md5CheckingEnabled: Boolean,
                                             val fileExistChecksEnabled: Boolean) {

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
        private var logger: Logger = FetchLogger(DEFAULT_LOGGING_ENABLED, DEFAULT_TAG)
        private var autoStart = DEFAULT_AUTO_START
        private var retryOnNetworkGain = DEFAULT_RETRY_ON_NETWORK_GAIN
        private var fileServerDownloader: FileServerDownloader = defaultFileServerDownloader
        private var md5CheckEnabled = DEFAULT_MD5_CHECK_ENABLED
        private var fileExistChecksEnabled = DEFAULT_FILE_EXIST_CHECKS

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
        fun setHttpDownloader(downloader: Downloader): Builder {
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
         * Default value is 1. This method can only accept values greater than 0. Setting
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

        /** Allows Fetch to check the fully downloaded file md5 checksum after the download completes
         * with md5 checksum returned by the server if supported. This check is only ran once.
         * Default is false
         * @param md5 md5 checking enabled
         * @return Builder
         * */
        fun enableMd5Check(enabled: Boolean): Builder {
            this.md5CheckEnabled = enabled
            return this
        }

        /**
         * Allows Fetch to check if the file exist for a download. If the file does not exist,
         * Fetch will update the database and queued, paused or downloading downloads will
         * have to start at the beginning. Enabled by default. Set to false only if you are
         * monitoring the file existence elsewhere.
         * */
        fun enableFileExistChecks(enabled: Boolean): Builder {
            this.fileExistChecksEnabled = enabled
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
                    md5CheckingEnabled = md5CheckEnabled,
                    fileExistChecksEnabled = fileExistChecksEnabled)
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
        if (md5CheckingEnabled != other.md5CheckingEnabled) return false
        if (fileExistChecksEnabled != other.fileExistChecksEnabled) return false
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
        result = 31 * result + md5CheckingEnabled.hashCode()
        result = 31 * result + fileExistChecksEnabled.hashCode()
        return result
    }

    override fun toString(): String {
        return "FetchConfiguration(appContext=$appContext, namespace='$namespace'," +
                " concurrentLimit=$concurrentLimit, progressReportingIntervalMillis=$progressReportingIntervalMillis," +
                " loggingEnabled=$loggingEnabled, httpDownloader=$httpDownloader, globalNetworkType=$globalNetworkType," +
                " logger=$logger, autoStart=$autoStart, retryOnNetworkGain=$retryOnNetworkGain, " +
                "fileServerDownloader=$fileServerDownloader, md5CheckingEnabled=$md5CheckingEnabled," +
                " fileExistChecksEnabled=$fileExistChecksEnabled)"
    }

}