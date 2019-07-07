package com.tonyodev.fetch2rx

import android.annotation.SuppressLint
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchModulesBuilder
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_NOTIFY_ON_REQUEST_UPDATED
import com.tonyodev.fetch2core.*

/**
 * A light weight file download manager for Android with Rx features.
 * Features: Background downloading,
 *           Queue based Priority downloading,
 *           Pause & Resume downloads,
 *           Network specific downloading and more...
 * @see https://github.com/tonyofrancis/Fetch
 * */
interface RxFetch {

    /** Returns true if this instance of fetch is closed and cannot be reused.*/
    val isClosed: Boolean

    /** The namespace which this instance of fetch operates in. An app can
     * have several instances of Fetch with different namespaces.
     * @see com.tonyodev.fetch2.FetchConfiguration
     * */
    val namespace: String

    /** Get the FetchConfiguration object that created this instance of Fetch.
     * Note: If you have updated settings on this instance of Fetch, this object
     * will not have these updated settings.
     * */
    val fetchConfiguration: FetchConfiguration

    /**
     * Queues a request for downloading. If Fetch fails to enqueue the request,
     * func2 will be called with the error.
     * Errors that may cause Fetch to fail the enqueue are :
     * 1. No storage space on the device.
     * 2. Fetch is already managing the same request. This means that a request with the same url
     * and file name is already managed.
     * @param request Download Request
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with request result.
     * Fetch may update a request depending on the initial request's Enqueue Action.
     * Update old request references with this request.
     * */
    fun enqueue(request: Request): Convertible<Request>

    /**
     * Queues a list of requests for downloading. If Fetch fails to enqueue a
     * download request because an error occurred, all other request in the list will
     * fail. Func2 will be called with the error message.
     * Errors that may cause Fetch to fail the enqueue are :
     * 1. No storage space on the device.
     * 2. Fetch is already managing the same request. This means that a request with the same url
     * and file name is already managed.
     * @param requests Request List
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with requests list. Returns a list with a pair<Request,Error> for each enqueued request.
     *         If the pair's second parameter is Error.NONE. this indicates that the request
     *         was enqueued successfully. If the Error is not ERROR.NONE. This indicates
     *         that the request was not enqueued for the specified reason.
     * Fetch may update a request depending on the initial request's Enqueue Action.
     * Update old request references with this request.
     * */
    fun enqueue(requests: List<Request>): Convertible<List<Pair<Request, Error>>>

    /** Pause a queued or downloading download.
     * @param ids ids of downloads to be paused.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of paused downloads. Note. Only downloads that
     * were paused will be returned in the result list.
     * */
    fun pause(ids: List<Int>): Convertible<List<Download>>

    /** Pause a queued or downloading download.
     * @param id id of download to be paused.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with paused download if successful otherwise null.
     * */
    fun pause(id: Int): Convertible<Download>

    /**
     * Pause all queued or downloading downloads within the specified group.
     * @param id specified group id.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that was paused.
     * */
    fun pauseGroup(id: Int): Convertible<List<Download>>

    /** Pauses all currently downloading items, and pauses all download processing fetch operations.
     *  Use this method when you do not want Fetch to keep processing downloads
     *  but do not want to release the instance of Fetch. However, you are still able to query
     *  download information.
     *  @see unfreeze
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with action results. True if freeze otherwise false.
     * */
    fun freeze(): Convertible<Boolean>

    /** Resume a download that has been paused.
     * @param ids ids of downloads to be resumed.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that was successfully resumed.
     * */
    fun resume(ids: List<Int>): Convertible<List<Download>>

    /** Resume a download that has been paused.
     * @param id id of download to be resumed.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with resumed download that was paused.
     * */
    fun resume(id: Int): Convertible<Download>

    /**
     * Resume all paused downloads within the specified group.
     * @param id specified group id.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of resumed downloads.
     * */
    fun resumeGroup(id: Int): Convertible<List<Download>>

    /** Allow fetch to resume operations after freeze has been called.
     * @see freeze
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with action results. True if unfreeze otherwise false.
     * */
    fun unfreeze(): Convertible<Boolean>

    /**
     * Remove a list of downloads managed by this instance of Fetch.
     * The downloaded file for the removed downloads are not deleted.
     * @param ids ids of downloads to be removed.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were removed.
     * */
    fun remove(ids: List<Int>): Convertible<List<Download>>

    /**
     * Remove a download managed by this instance of Fetch.
     * The downloaded file for the removed download is not deleted.
     * @param id id of download to be removed.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with download that was removed if successful.
     * */
    fun remove(id: Int): Convertible<Download>

    /**
     * Remove all downloads in the specified group managed by this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param id specified group id
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were removed.
     * */
    fun removeGroup(id: Int): Convertible<List<Download>>

    /**
     * Remove all downloads managed by this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were removed.
     * */
    fun removeAll(): Convertible<List<Download>>

    /**
     * Remove all downloads with the specified status in this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param status status
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were removed.
     * */
    fun removeAllWithStatus(status: Status): Convertible<List<Download>>

    /**
     * Remove all downloads with the specified group and status in this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param statuses statuses
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were removed.
     * */
    fun removeAllInGroupWithStatus(id: Int, statuses: List<Status>): Convertible<List<Download>>

    /**
     * Delete a list of downloads managed by this instance of Fetch.
     * The downloaded file is deleted.
     * @param ids ids of downloads to be deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were deleted.
     * */
    fun delete(ids: List<Int>): Convertible<List<Download>>

    /**
     * Delete a download managed by this instance of Fetch.
     * The downloaded file is deleted.
     * @param id id of download to be deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with download that was deleted if successful.
     * */
    fun delete(id: Int): Convertible<Download>

    /**
     * Deletes all downloads in the specified group managed by this instance of Fetch.
     * The downloaded files are also deleted.
     * @param id specified group id
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were deleted.
     * */
    fun deleteGroup(id: Int): Convertible<List<Download>>

    /**
     * Deletes all downloads managed by this instance of Fetch.
     * The downloaded files are deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were deleted.
     * */
    fun deleteAll(): Convertible<List<Download>>

    /**
     * Deletes all downloads with the specified status in this instance of Fetch.
     * The downloaded files are also deleted.
     * @param status status
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were deleted.
     * */
    fun deleteAllWithStatus(status: Status): Convertible<List<Download>>

    /**
     * Deletes all downloads with the specified group and status in this instance of Fetch.
     * The downloaded files are also deleted.
     * @param statuses statuses
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were deleted.
     * */
    fun deleteAllInGroupWithStatus(id: Int, statuses: List<Status>): Convertible<List<Download>>

    /**
     * Cancel a list of non completed downloads managed by this instance of Fetch.
     * The downloaded file for the cancelled download is not deleted.
     * @param ids ids of downloads to be cancelled.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were cancelled.
     * */
    fun cancel(ids: List<Int>): Convertible<List<Download>>

    /**
     * Cancel a non completed download managed by this instance of Fetch.
     * The downloaded file for the cancelled download is not deleted.
     * @param id id of downloads to be cancelled.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with download that was cancelled if successful.
     * */
    fun cancel(id: Int): Convertible<Download>

    /**
     * Cancels all non completed downloads in the specified group managed by this instance of Fetch.
     * The downloaded files for cancelled downloads are not deleted.
     * @param id specified group id
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were cancelled.
     * */
    fun cancelGroup(id: Int): Convertible<List<Download>>

    /**
     * Cancels all non completed downloads managed by this instance of Fetch.
     * The downloaded files for cancelled downloads are not deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with list of downloads that were cancelled.
     * */
    fun cancelAll(): Convertible<List<Download>>

    /**
     * Retries to download a list of failed or cancelled downloads.
     * @param ids ids of the failed or cancelled downloads.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with the list of downloads that were successfully queued.
     * */
    fun retry(ids: List<Int>): Convertible<List<Download>>

    /**
     * Retries to download a failed or cancelled download.
     * @param id id of the failed or cancelled downloads.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with the download that was successfully queued or null.
     * */
    fun retry(id: Int): Convertible<Download>

    /**
     * Resets the autoRetryAttempts value for a download back to 0.
     * @param downloadId Id of existing request/download
     * @param retryDownload Retry the download if its status is Status.ERROR. True by default.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with the download that was successfully queued or null.
     * */
    fun resetAutoRetryAttempts(downloadId: Int, retryDownload: Boolean = true): Convertible<Download?>

    /** Updates an existing request.
     * @see com.tonyodev.fetch2.Request for more details.
     * @param requestId Id of existing request/download
     * @param updatedRequest Request object
     * @param notifyListeners If the request is successfully updated notify attached Fetch listeners of the download status. Default true
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with the successfully updated download or null.
     * */
    fun updateRequest(requestId: Int, updatedRequest: Request, notifyListeners: Boolean = DEFAULT_ENABLE_LISTENER_NOTIFY_ON_REQUEST_UPDATED): Convertible<Download>

    /**
     * Renames the file for a completed download. The StorageResolver attached to this fetch instance will rename the file.
     * So it is okay to parse uri strings for the newFileName.
     * @param id Id of existing request/download
     * @param newFileName the new file name.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with the successfully updated download or null.
     * */
    fun renameCompletedDownloadFile(id: Int, newFileName: String): Convertible<Download>

    /** Replaces the existing extras object associated with an existing download/request with the newly passed in extras object.
     * @param id Id of existing request/download
     * @param extras new extras object
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with the successfully updated download or null.
     * */
    fun replaceExtras(id: Int, extras: Extras): Convertible<Download>

    /**
     * Gets all downloads managed by this instance of Fetch.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with results.
     * */
    fun getDownloads(): Convertible<List<Download>>

    /**
     * Gets the downloads which match an id in the list. Only successful matches will be returned.
     * @param idList Id list to perform id query against.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with results.
     * */
    fun getDownloads(idList: List<Int>): Convertible<List<Download>>

    /**
     * Gets the download which has the specified id. If the download
     * does not exist null will be returned.
     * @param id Download id
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with result.
     * */
    fun getDownload(id: Int): Convertible<Download?>

    /**
     * Gets all downloads in the specified group.
     * @param groupId group id to query.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with results.
     * */
    fun getDownloadsInGroup(groupId: Int): Convertible<List<Download>>

    /**
     * Gets all downloads with a specific status.
     * @see com.tonyodev.fetch2.Status
     * @param status Status to query.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with results.
     * */
    fun getDownloadsWithStatus(status: Status): Convertible<List<Download>>

    /**
     * Gets all downloads in a specific group with a specific status.
     * @see com.tonyodev.fetch2.Status
     * @param groupId group id to query.
     * @param statuses Statuses to query.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with results.
     * */
    fun getDownloadsInGroupWithStatus(groupId: Int, status: List<Status>): Convertible<List<Download>>

    /**
     * Gets all downloads containing the identifier.
     * @param identifier identifier.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with results.
     * */
    fun getDownloadsByRequestIdentifier(identifier: Long): Convertible<List<Download>>

    /**
     * Gets the FetchGroup by id. Even if the database does not contain downloads with this group id
     * a FetchGroup will be returned. It will contain no downloads however. When a download with this
     * group id is added. The downloads field on this object will be update and attached FetchObservers will be notified.
     * @param group the group id
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with results.
     * */
    fun getFetchGroup(group: Int): Convertible<FetchGroup>

    /**
     * Gets a list of the ids of all groups managed my this fetch namespace.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with results.
     * */
    fun getAllGroupIds(): Convertible<List<Int>>

    /**
     * Gets all downloads containing the tag.
     * @param tag tag.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with results.
     * */
    fun getDownloadsByTag(tag: String): Convertible<List<Download>>

    /** Attaches a FetchListener to this instance of Fetch.
     * @param listener Fetch Listener
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun addListener(listener: FetchListener): RxFetch

    /** Attaches a FetchListener to this instance of Fetch.
     * @param listener Fetch Listener
     * @param notify Allows Fetch to notify the newly attached listener instantly of the download status
     * of all downloads managed by the namespace. Default is false.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun addListener(listener: FetchListener, notify: Boolean = DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED): RxFetch

    /** Attaches a FetchListener to this instance of Fetch.
     * @param listener Fetch Listener
     * @param notify Allows Fetch to notify the newly attached listener instantly of the download status
     * of all downloads managed by the namespace. Default is false.
     * @param autoStart Allows Fetch to start processing requests if it is not already doing so.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun addListener(listener: FetchListener, notify: Boolean = DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED, autoStart: Boolean): RxFetch

    /** Detaches a FetchListener from this instance of Fetch.
     * @param listener Fetch Listener
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeListener(listener: FetchListener): RxFetch

    /**
     * Adds a completed download to Fetch for management. If Fetch is already managing another download with the same file as this completed download's
     * file, Fetch will replace the already managed download with this completed download.
     * @param completedDownload Completed Download
     * @param alertListeners boolean indicating whether to alert all listeners attached to this fetch's namespace of the downloads completed status.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun addCompletedDownload(completedDownload: CompletedDownload, alertListeners: Boolean = true): Convertible<Download>

    /**
     * Adds a list of completed downloads to Fetch for management. If Fetch is already managing another download with the same file as this completed download's
     * file, Fetch will replace the already managed download with this completed download.
     * @param completedDownloads Completed Downloads list
     * @param alertListeners boolean indicating whether to alert all listeners attached to this fetch's namespace of the downloads completed status.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible
     * */
    fun addCompletedDownloads(completedDownloads: List<CompletedDownload>, alertListeners: Boolean = true): Convertible<List<Download>>

    /**
     * Gets the list of download blocks belonging to a download. List may be empty if
     * blocks could not be found for the download id or download has never been processed.
     * @param downloadId: Download ID
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible
     * */
    fun getDownloadBlocks(downloadId: Int): Convertible<List<DownloadBlock>>

    /**
     * Enable or disable logging.
     * @param enabled Enable or disable logging.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun enableLogging(enabled: Boolean): RxFetch

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
    fun setGlobalNetworkType(networkType: NetworkType): RxFetch

    /** Sets the number of parallel downloads Fetch should perform at any given time.
     * Default value is 1. This method can only accept values greater than 0. Setting
     * concurrent limit to zero prevents the instance of Fetch to pull and download request
     * from the waiting queue but allows the instance of Fetch to act on and observe changes to
     * requests/downloads.
     * @param downloadConcurrentLimit Number of parallel downloads.
     * @throws FetchException if the passed in download concurrent limit is less than 0 or
     * Fetch instance has been closed.
     * @return Instance
     * */
    fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int): RxFetch

    /** Releases held resources and the namespace used by this Fetch instance.
     * Once closed this instance cannot be reused but the namespace can be reused
     * by a new instance of Fetch.
     * */
    fun close()

    /** Gets the set of FetchListeners attached to this instance.
     * @return set of attached FetchListeners.
     * */
    fun getListenerSet(): Set<FetchListener>

    /**
     * Gets the content Length for a request. If the request or contentLength cannot be found in
     * the Fetch database(meaning Fetch never processed the request and started downloading it) -1 is returned.
     * However, setting fromServer to true will create a new connection to the server to get the connectLength
     * if Fetch does not already contain the data in the database for the request.
     * @param request Request. Can be a managed or un-managed request. The request is not stored in
     * the fetch database.
     * connection to get the contentLength
     * @param fromServer If true, fetch will attempt to get the ContentLength
     * from the server directly by making a network request. Otherwise no action is taken.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with content length result. If value is -1. This means that Fetch was
     * not able to get the content length.
     * */
    fun getContentLengthForRequest(request: Request, fromServer: Boolean): Convertible<Long>

    /**
     * Gets the content Length for each request in the passed in list. If the request or contentLength cannot be found in
     * the Fetch database(meaning Fetch never processed the request and started downloading it) -1 is returned.
     * However, setting fromServer to true will create a new connection to the server to get the connectLength
     * if Fetch does not already contain the data in the database for the request.
     * @param requests Request list. Can be a managed or un-managed list of requests. The requests are not stored in
     * the fetch database.
     * @param fromServer If true, fetch will attempt to get the ContentLength
     * from the server directly by making a network request. Otherwise no action is taken.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return convertible containing the success and error result list.
     * */
    fun getContentLengthForRequests(requests:List<Request>, fromServer: Boolean): Convertible<Pair<List<Pair<Request, Long>>, List<Pair<Request, Error>>>>

    /**
     * Gets the Server Response for the url and associated headers.
     * @param url the url. Cannot be null.
     * @param headers the request headers for the url. Can be null.
     * @throws FetchException if this instance of Fetch has been closed.
     * @throws IOException if the the server request was not successful.
     * @return Convertible with the server response.
     * */
    fun getServerResponse(url: String, headers: Map<String, String>?): Convertible<Downloader.Response>

    /**
     * Gets the full Catalog of a Fetch File Server.
     * @param request Request. Can be a managed or un-managed request. The request is not stored in
     * the fetch database.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Convertible with catalog results.
     * */
    fun getFetchFileServerCatalog(request: Request): Convertible<List<FileResource>>

    /**
     * Blocks the current thread(Not Ui Thread) to waiting on one of the two conditions.
     * Condition One: Waits until Fetch has downloaded all downloading and pending downloads.
     * Condition Two: Waits until the allow time expires
     * if Fetch has not completed or attempted to download queued downloads.
     * This method returns when one of the conditions if violated.
     * Note: Calling this method on the UIThread is strongly discouraged and an exception is thrown.
     * @param allowTimeInMilliseconds the allowed time in milliseconds. If zero the wait is indefinite.
     * @throws FetchException if calling on the main thread
     * */
    fun awaitFinishOrTimeout(allowTimeInMilliseconds: Long)

    /**
     * Blocks the current thread(Not Ui Thread) to waiting on the current conditions.
     * Condition One: Waits until Fetch has downloaded all downloading and pending downloads.
     * This method returns when one of the conditions if violated.
     * Note: Calling this method on the UIThread is strongly discouraged and an exception is thrown.
     * @throws FetchException if calling on the main thread
     * */
    fun awaitFinish()

    /**
     * Attaches a FetchObserver to listen for changes on a download managed by this Fetch namespace.
     * FetchObservers are held with a weak reference. Note: If fetch does not manage a download with
     * the passed in id, the FetchObserver will not be notified. Only when a download with the specified
     * id is managed by Fetch will the observer be called.
     * @param downloadId the download Id
     * @param fetchObservers the fetch observers
     * @throws FetchException if this instance of Fetch has been closed.
     * @return instance
     * */
    fun attachFetchObserversForDownload(downloadId: Int, vararg fetchObservers: FetchObserver<Download>): RxFetch

    /**
     * Removes a FetchObserver attached to this Fetch namespace for a download.
     * @param downloadId the download Id
     * @param fetchObservers the fetch observers
     * @throws FetchException if this instance of Fetch has been closed.
     * @return instance
     * */
    fun removeFetchObserversForDownload(downloadId: Int, vararg fetchObservers: FetchObserver<Download>): RxFetch

    /** Indicates if this fetch namespace has active(Queued or Downloading) downloads. You can use this value to
     * keep a background service ongoing until the results returns false.
     * @param includeAddedDownloads To include downloads with a status of Added. Added downloads are not considered active.
     * @throws FetchException if accessed on ui thread
     * @return Convertible with results.
     * */
    fun hasActiveDownloads(includeAddedDownloads: Boolean): Convertible<Boolean>

    /** Subscribe a FetchObserver that indicates if this fetch namespace has active(Queued or Downloading) downloads. You can use this value to
     * keep a background service ongoing until the value returned is false.
     * @param includeAddedDownloads To include downloads with a status of Added. Added downloads are not considered active by default.
     * @param fetchObserver the fetch observer
     * @throws FetchException if this instance of Fetch has been closed.
     * @return instance
     * */
    fun addActiveDownloadsObserver(includeAddedDownloads: Boolean = false, fetchObserver: FetchObserver<Boolean>): RxFetch

    /** Removes a subscribed FetchObserver that is listening for active downloads.
     * @param fetchObserver the fetch observer to remove.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return instance
     * */
    fun removeActiveDownloadsObserver(fetchObserver: FetchObserver<Boolean>): RxFetch

    /**
     * RX Fetch implementation class. Use this Singleton to get instances of RxFetch or Fetch.
     * */
    companion object Impl {

        private val lock = Any()
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var defaultRxFetchConfiguration: FetchConfiguration? = null
        @Volatile
        private var defaultRxFetchInstance: RxFetch? = null

        /**
         * Sets the default Configuration settings on the default Fetch instance.
         * @param fetchConfiguration custom Fetch Configuration
         * */
        fun setDefaultRxInstanceConfiguration(fetchConfiguration: FetchConfiguration) {
            synchronized(lock) {
                defaultRxFetchConfiguration = fetchConfiguration
            }
        }

        /**
         * Get the default Fetch Configuration set with setDefaultInstanceConfiguration(fetchConfiguration: FetchConfiguration)
         * or setDefaultInstanceConfiguration(context: Context)
         * @return default FetchConfiguration
         * */
        fun getDefaultRxFetchConfiguration(): FetchConfiguration? {
            return synchronized(lock) {
                defaultRxFetchConfiguration
            }
        }

        /**
         * @throws FetchException if default FetchConfiguration is not set.
         * @return Get default RxFetch instance
         * */
        fun getDefaultRxInstance(): RxFetch {
            return synchronized(lock) {
                val rxFetchConfiguration = defaultRxFetchConfiguration
                        ?: throw FetchException(GLOBAL_FETCH_CONFIGURATION_NOT_SET)
                val defaultRxFetch = defaultRxFetchInstance
                if (defaultRxFetch == null || defaultRxFetch.isClosed) {
                    val newDefaultRxFetch = RxFetchImpl.newInstance(FetchModulesBuilder.buildModulesFromPrefs(rxFetchConfiguration))
                    defaultRxFetchInstance = newDefaultRxFetch
                    newDefaultRxFetch
                } else {
                    defaultRxFetch
                }
            }
        }

        /**
         * Creates a custom Instance of Fetch with the given configuration and namespace.
         * @param fetchConfiguration custom Fetch Configuration
         * @return custom RxFetch instance
         * */
        fun getRxInstance(fetchConfiguration: FetchConfiguration): RxFetch {
            return RxFetchImpl.newInstance(FetchModulesBuilder.buildModulesFromPrefs(fetchConfiguration))
        }

    }

}