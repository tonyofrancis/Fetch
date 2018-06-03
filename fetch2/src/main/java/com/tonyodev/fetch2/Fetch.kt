package com.tonyodev.fetch2

import android.annotation.SuppressLint
import android.content.Context
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchImpl
import com.tonyodev.fetch2.fetch.FetchModulesBuilder
import com.tonyodev.fetch2.util.DEFAULT_INSTANCE_NAMESPACE
import com.tonyodev.fetch2.util.GLOBAL_FETCH_CONFIGURATION_NOT_SET
import com.tonyodev.fetch2.util.createConfigWithNewNamespace


/**
 * A light weight file download manager for Android.
 * Features: Background downloading,
 *           Queue based Priority downloading,
 *           Pause & Resume downloads,
 *           Network specific downloading and more...
 * */
interface Fetch {

    /** Returns true if this instance of fetch is closed and cannot be reused.*/
    val isClosed: Boolean

    /** The namespace which this instance of fetch operates in. An app can
     * have several instances of Fetch with different namespaces.
     * @see com.tonyodev.fetch2.Fetch.Builder
     * */
    val namespace: String

    /**
     * Queues a request for downloading. If Fetch fails to enqueue the request,
     * func2 will be called with the error message.
     * Errors that may cause Fetch to fail the enqueue are :
     * 1. No storage space on the device.
     * 2. Fetch is already managing the same request. This means that a request with the same url
     * and file name is already managed.
     * @param request Download Request
     * @param func Callback that the enqueued download results will be returned on.
     *             If using Request Options with Fetch, the download object file and ID values may be different
     *             from the initial request object file and ID values when enqueuing.
     *             Update all external references accordingly.
     * @param func2 Callback that is called when enqueuing a request fails. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun enqueue(request: Request, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Queues a list of requests for downloading. If Fetch fails to enqueue a
     * download request because an error occurred, all other request in the list will
     * fail. Func2 will be called with the error message.
     * Errors that may cause Fetch to fail the enqueue are :
     * 1. No storage space on the device.
     * 2. Fetch is already managing the same request. This means that a request with the same url
     * and file name is already managed.
     * @param requests Request List
     * @param func Callback that the enqueued download results will be returned on.
     *             If using Request Options with Fetch, the download object file and ID values may be different
     *             from the initial request object file and ID values when enqueuing.
     *             Update all external references accordingly.
     * @param func2 Callback that is called when enqueuing a request fails. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun enqueue(requests: List<Request>, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /** Pause a queued or downloading download.
     * @param ids ids of downloads to be paused.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun pause(vararg ids: Int): Fetch

    /**
     * Pause all queued or downloading downloads within the specified group.
     * @param id specified group id.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun pauseGroup(id: Int): Fetch

    /** Pauses all currently downloading items, and pauses all download processing fetch operations.
     *  Use this method when you do not want Fetch to keep processing downloads
     *  but do not want to release the instance of Fetch. However, you are still able to query
     *  download information.
     *  @see unfreeze
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun freeze(): Fetch

    /** Resume a download that has been paused.
     * @param ids ids of downloads to be resumed.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun resume(vararg ids: Int): Fetch

    /**
     * Resume all paused downloads within the specified group.
     * @param id specified group id.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun resumeGroup(id: Int): Fetch

    /** Allow fetch to resume operations after freeze has been called.
     * @see freeze
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun unfreeze(): Fetch

    /**
     * Remove a download managed by this instance of Fetch.
     * The downloaded file for the removed download is not deleted.
     * @param ids ids of downloads to be removed.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun remove(vararg ids: Int): Fetch

    /**
     * Remove all downloads in the specified group managed by this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param id specified group id
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeGroup(id: Int): Fetch

    /**
     * Remove all downloads managed by this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeAll(): Fetch

    /**
     * Remove all downloads with the specified status in this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param status status
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeAllWithStatus(status: Status): Fetch

    /**
     * Delete a download managed by this instance of Fetch.
     * The downloaded file is deleted.
     * @param ids ids of downloads to be deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun delete(vararg ids: Int): Fetch

    /**
     * Deletes all downloads in the specified group managed by this instance of Fetch.
     * The downloaded files are also deleted.
     * @param id specified group id
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteGroup(id: Int): Fetch

    /**
     * Deletes all downloads managed by this instance of Fetch.
     * The downloaded files are deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteAll(): Fetch

    /**
     * Deletes all downloads with the specified status in this instance of Fetch.
     * The downloaded files are also deleted.
     * @param status status
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteAllWithStatus(status: Status): Fetch

    /**
     * Cancel a non completed download managed by this instance of Fetch.
     * The downloaded file for the cancelled download is not deleted.
     * @param ids ids of downloads to be cancelled.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun cancel(vararg ids: Int): Fetch

    /**
     * Cancels all non completed downloads in the specified group managed by this instance of Fetch.
     * The downloaded files for cancelled downloads are not deleted.
     * @param id specified group id
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun cancelGroup(id: Int): Fetch

    /**
     * Cancels all non completed downloads managed by this instance of Fetch.
     * The downloaded files for cancelled downloads are not deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun cancelAll(): Fetch

    /**
     * Retries to download a failed or cancelled download.
     * @param ids ids of the failed or cancelled downloads.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun retry(vararg ids: Int): Fetch

    /** Updates and replaces an existing download's groupId, headers, priority and network
     * type information.
     * @see com.tonyodev.fetch2.RequestInfo for more details.
     * @param id Id of existing download
     * @param requestInfo Request Info object
     * @param func Successful callback that the download will be returned on.
     * @param func2 Failed callback that the error will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun updateRequest(id: Int, requestInfo: RequestInfo, func: Func<Download>? = null,
                      func2: Func<Error>? = null): Fetch

    /**
     * Gets all downloads managed by this instance of Fetch.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloads(func: Func<List<Download>>): Fetch

    /**
     * Gets the downloads which match an id in the list. Only successful matches will be returned.
     * @param idList Id list to perform id query against.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloads(idList: List<Int>, func: Func<List<Download>>): Fetch

    /**
     * Gets the download which has the specified id. If the download
     * does not exist null will be returned.
     * @param id Download id
     * @param func Callback that the results will be returned on. Result maybe null.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownload(id: Int, func: Func2<Download?>): Fetch

    /**
     * Gets all downloads in the specified group.
     * @param groupId group id to query.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloadsInGroup(groupId: Int, func: Func<List<Download>>): Fetch

    /**
     * Gets all downloads with a specific status.
     * @see com.tonyodev.fetch2.Status
     * @param status Status to query.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloadsWithStatus(status: Status, func: Func<List<Download>>): Fetch

    /**
     * Gets all downloads in a specific group with a specific status.
     * @see com.tonyodev.fetch2.Status
     * @param groupId group id to query.
     * @param status Status to query.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloadsInGroupWithStatus(groupId: Int, status: Status, func: Func<List<Download>>): Fetch

    /** Attaches a FetchListener to this instance of Fetch.
     * @param listener Fetch Listener
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun addListener(listener: FetchListener): Fetch

    /** Detaches a FetchListener from this instance of Fetch.
     * @param listener Fetch Listener
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeListener(listener: FetchListener): Fetch

    /**
     * Enable or disable logging.
     * @param enabled Enable or disable logging.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun enableLogging(enabled: Boolean): Fetch

    /**
     * Overrides each downloads specific network type preference and uses a
     * global network type preference instead.
     * Use com.tonyodev.fetch2.NetworkType.GLOBAL_OFF to disable the global network preference.
     * The default value is com.tonyodev.fetch2.NetworkType.GLOBAL_OFF
     * @see com.tonyodev.fetch2.NetworkType
     * @param networkType The global network type.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun setGlobalNetworkType(networkType: NetworkType): Fetch

    /** Releases held resources and the namespace used by this Fetch instance.
     * Once closed this instance cannot be reused but the namespace can be reused
     * by a new instance of Fetch.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun close()

    /**
     * Fetch implementation class. Use this Singleton to get instances of Fetch.
     * */
    companion object Impl {

        private val lock = Any()
        @SuppressLint("StaticFieldLeak")
        private var defaultFetchConfiguration: FetchConfiguration? = null

        /**
         * Sets the default Configuration settings on the default Fetch instance.
         * @param context context
         * */
        fun setDefaultInstanceConfiguration(context: Context) {
            val config = FetchConfiguration.Builder(context)
                    .setNamespace(DEFAULT_INSTANCE_NAMESPACE)
                    .build()
            setDefaultInstanceConfiguration(config)
        }

        /**
         * Sets the default Configuration settings on the default Fetch instance.
         * @param fetchConfiguration custom Fetch Configuration
         * */
        fun setDefaultInstanceConfiguration(fetchConfiguration: FetchConfiguration) {
            synchronized(lock) {
                val config = if (fetchConfiguration.namespace != DEFAULT_INSTANCE_NAMESPACE) {
                    createConfigWithNewNamespace(fetchConfiguration, DEFAULT_INSTANCE_NAMESPACE)
                } else {
                    fetchConfiguration
                }
                defaultFetchConfiguration = config
            }
        }

        /**
         * Get the default Fetch Configuration set with setDefaultInstanceConfiguration(fetchConfiguration: FetchConfiguration)
         * or setDefaultInstanceConfiguration(context: Context)
         * @throws FetchException if default FetchConfiguration is not set.
         * @return default FetchConfiguration
         * */
        fun getDefaultFetchConfiguration(): FetchConfiguration {
            return synchronized(lock) {
                if (defaultFetchConfiguration == null) {
                    throw FetchException(GLOBAL_FETCH_CONFIGURATION_NOT_SET, FetchException.Code.GLOBAL_CONFIGURATION_NOT_SET)
                } else {
                    defaultFetchConfiguration!!
                }
            }
        }

        /**
         * @return Get default Fetch instance
         * */
        fun getDefaultInstance(): Fetch {
            synchronized(lock) {
                if (defaultFetchConfiguration == null) {
                    throw FetchException(GLOBAL_FETCH_CONFIGURATION_NOT_SET, FetchException.Code.GLOBAL_CONFIGURATION_NOT_SET)
                }
                return FetchImpl.newInstance(FetchModulesBuilder.buildModulesFromPrefs(defaultFetchConfiguration!!))
            }
        }

        /**
         * Creates a custom Instance of Fetch with the given configuration and namespace.
         * @param fetchConfiguration custom Fetch Configuration
         * @return custom Fetch instance
         * */
        fun getInstance(fetchConfiguration: FetchConfiguration): Fetch {
            return if (fetchConfiguration.namespace == DEFAULT_INSTANCE_NAMESPACE) {
                setDefaultInstanceConfiguration(fetchConfiguration)
                getDefaultInstance()
            } else {
                return FetchImpl.newInstance(FetchModulesBuilder.buildModulesFromPrefs(fetchConfiguration))
            }
        }

    }

}
