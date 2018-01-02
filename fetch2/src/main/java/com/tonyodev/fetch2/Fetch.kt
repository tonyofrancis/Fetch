package com.tonyodev.fetch2

import android.content.Context
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchBuilder
import com.tonyodev.fetch2.fetch.FetchImpl
import com.tonyodev.fetch2.fetch.FetchModulesBuilder

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
     * @param func2 Callback that is called when enqueuing a request fails. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun enqueue(request: Request, func: Func<Download>? = null, func2: Func<Error>? = null)

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
     * @param func2 Callback that is called when enqueuing a request fails. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun enqueue(requests: List<Request>, func: Func<List<Download>>? = null, func2: Func<Error>? = null)

    /** Pause a queued or downloading download.
     * @param ids ids of downloads to be paused.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun pause(vararg ids: Int)

    /**
     * Pause all queued or downloading downloads within the specified group.
     * @param id specified group id.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun pauseGroup(id: Int)

    /** Pauses all currently downloading items, and pauses all download processing fetch operations.
     *  Use this method when you do not want Fetch to keep processing downloads
     *  but do not want to release the instance of Fetch. However, you are still able to query
     *  download information.
     *  @see unfreeze
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun freeze()

    /** Resume a download that has been paused.
     * @param ids ids of downloads to be resumed.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun resume(vararg ids: Int)

    /**
     * Resume all paused downloads within the specified group.
     * @param id specified group id.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun resumeGroup(id: Int)

    /** Allow fetch to resume operations after freeze has been called.
     * @see freeze
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun unfreeze()

    /**
     * Remove a download managed by this instance of Fetch.
     * The downloaded file for the removed download is not deleted.
     * @param ids ids of downloads to be removed.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun remove(vararg ids: Int)

    /**
     * Remove all downloads in the specified group managed by this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param id specified group id
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun removeGroup(id: Int)

    /**
     * Remove all downloads managed by this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun removeAll()

    /**
     * Delete a download managed by this instance of Fetch.
     * The downloaded file is deleted.
     * @param ids ids of downloads to be deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun delete(vararg ids: Int)

    /**
     * Deletes all downloads in the specified group managed by this instance of Fetch.
     * The downloaded files are also deleted.
     * @param id specified group id
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun deleteGroup(id: Int)

    /**
     * Deletes all downloads managed by this instance of Fetch.
     * The downloaded files are deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun deleteAll()

    /**
     * Cancel a non completed download managed by this instance of Fetch.
     * The downloaded file for the cancelled download is not deleted.
     * @param ids ids of downloads to be cancelled.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun cancel(vararg ids: Int)

    /**
     * Cancels all non completed downloads in the specified group managed by this instance of Fetch.
     * The downloaded files for cancelled downloads are not deleted.
     * @param id specified group id
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun cancelGroup(id: Int)

    /**
     * Cancels all non completed downloads managed by this instance of Fetch.
     * The downloaded files for cancelled downloads are not deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun cancelAll()

    /**
     * Retries to download a failed or cancelled download.
     * @param ids ids of the failed or cancelled downloads.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun retry(vararg ids: Int)

    /** Updates and replaces an existing download's groupId, headers, priority and network
     * type information.
     * @see com.tonyodev.fetch2.RequestInfo for more details.
     * @param id Id of existing download
     * @param requestInfo Request Info object
     * @param func Successful callback that the download will be returned on.
     * @param func2 Failed callback that the error will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun updateRequest(id: Int, requestInfo: RequestInfo, func: Func<Download>? = null,
                      func2: Func<Error>? = null)

    /**
     * Gets all downloads managed by this instance of Fetch.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun getDownloads(func: Func<List<Download>>)

    /**
     * Gets the downloads which match an id in the list. Only successful matches will be returned.
     * @param idList Id list to perform id query against.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun getDownloads(idList: List<Int>, func: Func<List<Download>>)

    /**
     * Gets the download which has the specified id. If the download
     * does not exist null will be returned.
     * @param id Download id
     * @param func Callback that the results will be returned on. Result maybe null.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun getDownload(id: Int, func: Func2<Download?>)

    /**
     * Gets all downloads in the specified group.
     * @param groupId group id to query.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun getDownloadsInGroup(groupId: Int, func: Func<List<Download>>)

    /**
     * Gets all downloads with a specific status.
     * @see com.tonyodev.fetch2.Status
     * @param status Status to query.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun getDownloadsWithStatus(status: Status, func: Func<List<Download>>)

    /**
     * Gets all downloads in a specific group with a specific status.
     * @see com.tonyodev.fetch2.Status
     * @param groupId group id to query.
     * @param status Status to query.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun getDownloadsInGroupWithStatus(groupId: Int, status: Status, func: Func<List<Download>>)

    /** Attaches a FetchListener to this instance of Fetch.
     * @param listener Fetch Listener
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun addListener(listener: FetchListener)

    /** Detaches a FetchListener from this instance of Fetch.
     * @param listener Fetch Listener
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun removeListener(listener: FetchListener)

    /**
     * Enable or disable logging.
     * @param enabled Enable or disable logging.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun enableLogging(enabled: Boolean)

    /**
     * Overrides each downloads specific network type preference and uses a
     * global network type preference instead.
     * Use com.tonyodev.fetch2.NetworkType.GLOBAL_OFF to disable the global network preference.
     * The default value is com.tonyodev.fetch2.NetworkType.GLOBAL_OFF
     * @see com.tonyodev.fetch2.NetworkType
     * @param networkType The global network type.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun setGlobalNetworkType(networkType: NetworkType)

    /** Releases held resources and the namespace used by this Fetch instance.
     * Once closed this instance cannot be reused but the namespace can be reused
     * by a new instance of Fetch.
     * @throws FetchException if this instance of Fetch has been closed.
     * */
    fun close()

    /**
     * Builder used to configure and create an instance of Fetch.
     * */
    open class Builder constructor(
            /** Context*/
            context: Context,

            /** The namespace which Fetch operates in. Fetch uses
             * a namespace to create a database that the instance will use. Downloads
             * enqueued on the instance will belong to the namespace and will not be accessible
             * from any other namespace. An App can only have one Active Fetch instance with the
             * specified namespace. * In essence an App can have many instances of fetch
             * with a different namespaces.
             * */
            namespace: String)
        : FetchBuilder<Builder, Fetch>(context, namespace) {

        /** Builds a new instance of Fetch with the proper configuration.
         * @throws FetchException if an active instance of Fetch with the same namespace already
         * exists.
         * @return New instance of Fetch.
         * */
        override fun build(): Fetch {
            val modules = FetchModulesBuilder.buildModulesFromPrefs(getBuilderPrefs())
            return FetchImpl.newInstance(modules)
        }

    }

}