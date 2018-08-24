package com.tonyodev.fetch2

import android.annotation.SuppressLint
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchImpl
import com.tonyodev.fetch2.fetch.FetchModulesBuilder
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED
import com.tonyodev.fetch2core.*

/**
 * A light weight file download manager for Android.
 * Features: Background downloading,
 *           Queue based Priority downloading,
 *           Pause & Resume downloads,
 *           Network specific downloading and more...
 * @see https://github.com/tonyofrancis/Fetch
 * */
interface Fetch {

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

    /** Indicates if this fetch namespace has active(Queued or Downloading) downloads. You can use this value to
     * keep a background service using fetch ongoing until this field returns false.
     * This field can be accessed from any thread.
     * */
    val hasActiveDownloads: Boolean

    /**
     * Queues a request for downloading. If Fetch fails to enqueue the request,
     * func2 will be called with the error.
     * Errors that may cause Fetch to fail the enqueue are :
     * 1. No storage space on the device.
     * 2. Fetch is already managing the same request. This means that a request with the same url
     * and file name is already managed.
     * @param request Download Request
     * @param func Callback that the enqueued request will be returned on.
     *             Fetch may update a request depending on the initial request's Enqueue Action.
     *             Update old request references with this request.
     * @param func2 Callback that is called when enqueuing a request fails. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun enqueue(request: Request, func: Func<Request>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Queues a list of requests for downloading. If Fetch fails to enqueue a
     * download request because an error occurred, all other request in the list will
     * fail. Func2 will be called with the error message.
     * Errors that may cause Fetch to fail the enqueue are :
     * 1. No storage space on the device.
     * 2. Fetch is already managing the same request. This means that a request with the same url
     * and file name is already managed.
     * @param requests Request List
     * @param func Callback that the enqueued request will be returned on.
     *             Fetch may update a request depending on the initial request's Enqueue Action.
     *             Update old request references with this request.
     * @param func2 Callback that is called when enqueuing a request fails. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun enqueue(requests: List<Request>, func: Func<List<Request>>? = null, func2: Func<Error>? = null): Fetch

    /** Pause a queued or downloading download.
     * @param ids ids of downloads to be paused.
     * @param func Callback the paused downloads will be returned on. Note. Only downloads that
     * were paused will be returned in the result list.
     * @param func2 Callback that is called when attempting to pause downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun pause(ids: List<Int>, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /** Pause a queued or downloading download.
     * @param ids ids of downloads to be paused.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun pause(ids: List<Int>): Fetch

    /** Pause a queued or downloading download.
     * @param id id of download to be paused.
     * @param func Callback where the paused download will be returned on if successful.
     * @param func2 Callback that is called when attempting to pause downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun pause(id: Int, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

    /** Pause a queued or downloading download.
     * @param id id of download to be paused.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun pause(id: Int): Fetch

    /**
     * Pause all queued or downloading downloads within the specified group.
     * @param id specified group id.
     * @param func callback that returns list of downloads that were paused in the group.
     * @param func2 Callback that is called when attempting to pause downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun pauseGroup(id: Int, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

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
     *  @param func callback returning the success of the freeze.
     *  @param func2 callback used to report error.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun freeze(func: Func<Boolean>? = null, func2: Func<Error>? = null): Fetch

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
     * @param func callback where successfully resumed downloads will be returned.
     * @param func2 Callback that is called when attempting to resume downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun resume(ids: List<Int>, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /** Resume a download that has been paused.
     * @param ids ids of downloads to be resumed.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun resume(ids: List<Int>): Fetch

    /** Resume a download that has been paused.
     * @param id id of download to be resumed.
     * @param func callback where successfully resumed download will be returned.
     * @param func2 Callback that is called when attempting to resume downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun resume(id: Int, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

    /** Resume a download that has been paused.
     * @param id id of download to be resumed.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun resume(id: Int): Fetch

    /**
     * Resume all paused downloads within the specified group.
     * @param id specified group id.
     * @param func callback where successfully resumed downloads will be returned on.
     * @param func2 Callback that is called when attempting to resume downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun resumeGroup(id: Int, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Resume all paused downloads within the specified group.
     * @param id specified group id.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun resumeGroup(id: Int): Fetch

    /** Allow fetch to resume operations after freeze has been called.
     * @see freeze
     * @param func callback returning the success of the freeze.
     * @param func2 callback used to report error.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun unfreeze(func: Func<Boolean>? = null, func2: Func<Error>? = null): Fetch

    /** Allow fetch to resume operations after freeze has been called.
     * @see freeze
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun unfreeze(): Fetch

    /**
     * Remove a list of downloads managed by this instance of Fetch.
     * The downloaded file for the removed downloads are not deleted.
     * @param ids ids of downloads to be removed.
     * @param func callback used to report successfully removed downloads.
     * @param func2 Callback that is called when attempting to remove downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun remove(ids: List<Int>, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Remove a list of downloads managed by this instance of Fetch.
     * The downloaded file for the removed downloads are not deleted.
     * @param ids ids of downloads to be removed.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun remove(ids: List<Int>): Fetch

    /**
     * Remove a download managed by this instance of Fetch.
     * The downloaded file for the removed download is not deleted.
     * @param id id of download to be removed.
     * @param func callback used to report the successfully removed download.
     * @param func2 Callback that is called when attempting to remove downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun remove(id: Int, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Remove a download managed by this instance of Fetch.
     * The downloaded file for the removed download is not deleted.
     * @param id id of download to be removed.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun remove(id: Int): Fetch

    /**
     * Remove all downloads in the specified group managed by this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param id specified group id
     * @param func callback with results of removed downloads in the group that were removed.
     * @param func2 Callback that is called when attempting to remove downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeGroup(id: Int, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

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
     * @param func callback reporting all downloads that were removed.
     * @param func2 Callback that is called when attempting to remove downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeAll(func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

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
     * @param func callback returning a list of downloads that were removed.
     * @param func2 Callback that is called when attempting to remove downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeAllWithStatus(status: Status, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Remove all downloads with the specified status in this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param status status
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeAllWithStatus(status: Status): Fetch

    /**
     * Remove all downloads with the specified group and status in this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param status status
     * @param func callback returning a list of downloads that were removed.
     * @param func2 Callback that is called when attempting to remove downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeAllInGroupWithStatus(id: Int, status: Status, func: Func<List<Download>>?, func2: Func<Error>? = null): Fetch

    /**
     * Remove all downloads with the specified group and status in this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param status status
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeAllInGroupWithStatus(id: Int, status: Status): Fetch

    /**
     * Delete a list of downloads managed by this instance of Fetch.
     * The downloaded file is deleted.
     * @param ids ids of downloads to be deleted.
     * @param func callback that returns the downloads that were deleted.
     * @param func2 Callback that is called when attempting to delete downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun delete(ids: List<Int>, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Delete a list of downloads managed by this instance of Fetch.
     * The downloaded file is deleted.
     * @param ids ids of downloads to be deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun delete(ids: List<Int>): Fetch

    /**
     * Delete a download managed by this instance of Fetch.
     * The downloaded file is deleted.
     * @param id id of download to be deleted.
     * @param func callback that returns the successfully deleted download.
     * @param func2 Callback that is called when attempting to delete downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun delete(id: Int, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Delete a download managed by this instance of Fetch.
     * The downloaded file is deleted.
     * @param id id of download to be deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun delete(id: Int): Fetch

    /**
     * Deletes all downloads in the specified group managed by this instance of Fetch.
     * The downloaded files are also deleted.
     * @param id specified group id
     * @param func callback that returns the list of downloads that were deleted in the specified group.
     * @param func2 Callback that is called when attempting to delete downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteGroup(id: Int, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

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
     * @param func callback returns the result of all deleted downloads.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteAll(func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

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
     * @param func callback returns all deleted downloads with a specified status.
     * @param func2 Callback that is called when attempting to delete downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteAllWithStatus(status: Status, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Deletes all downloads with the specified status in this instance of Fetch.
     * The downloaded files are also deleted.
     * @param status status
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteAllWithStatus(status: Status): Fetch

    /**
     * Deletes all downloads with the specified group and status in this instance of Fetch.
     * The downloaded files are also deleted.
     * @param status status
     * @param func callback returns all deleted downloads with a specified status.
     * @param func2 Callback that is called when attempting to delete downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteAllInGroupWithStatus(id: Int, status: Status, func: Func<List<Download>>?, func2: Func<Error>? = null): Fetch

    /**
     * Deletes all downloads with the specified group and status in this instance of Fetch.
     * The downloaded files are also deleted.
     * @param status status
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteAllInGroupWithStatus(id: Int, status: Status): Fetch

    /**
     * Cancel a list of non completed downloads managed by this instance of Fetch.
     * The downloaded file for the cancelled download is not deleted.
     * @param ids ids of downloads to be cancelled.
     * @param func callback used to return the results of the cancelled downloads.
     * @param func2 Callback that is called when attempting to delete downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun cancel(ids: List<Int>, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Cancel a list of non completed downloads managed by this instance of Fetch.
     * The downloaded file for the cancelled download is not deleted.
     * @param ids ids of downloads to be cancelled.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun cancel(ids: List<Int>): Fetch

    /**
     * Cancel a non completed download managed by this instance of Fetch.
     * The downloaded file for the cancelled download is not deleted.
     * @param id id of downloads to be cancelled.
     * @param func callback used to return the successful cancelled download.
     * @param func2 Callback that is called when attempting to delete downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun cancel(id: Int, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Cancel a non completed download managed by this instance of Fetch.
     * The downloaded file for the cancelled download is not deleted.
     * @param id id of downloads to be cancelled.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun cancel(id: Int): Fetch

    /**
     * Cancels all non completed downloads in the specified group managed by this instance of Fetch.
     * The downloaded files for cancelled downloads are not deleted.
     * @param id specified group id
     * @param func callback that returns the list of cancelled downloads.
     * @param func2 Callback that is called when attempting to delete downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun cancelGroup(id: Int, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

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
     * @param func callback that returns the list of cancelled downloads.
     * @param func2 Callback that is called when attempting to delete downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun cancelAll(func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Cancels all non completed downloads managed by this instance of Fetch.
     * The downloaded files for cancelled downloads are not deleted.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun cancelAll(): Fetch

    /**
     * Retries to download a list of failed or cancelled downloads.
     * @param ids ids of the failed or cancelled downloads.
     * @param func callback that returns the list of downloads that were successfully queued.
     * @param func2 Callback that is called when attempting to retry downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun retry(ids: List<Int>, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Retries to download a failed or cancelled download.
     * @param ids ids of the failed or cancelled downloads.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun retry(ids: List<Int>): Fetch

    /**
     * Retries to download a failed or cancelled download.
     * @param id id of the failed or cancelled downloads.
     * @param func callback that returns the successful queued download.
     * @param func2 Callback that is called when attempting to retry downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun retry(id: Int, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Retries to download a failed or cancelled download.
     * @param id id of the failed or cancelled downloads.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun retry(id: Int): Fetch

    /** Updates an existing request.
     * @see com.tonyodev.fetch2.Request for more details.
     * @param requestId Id of existing request/download
     * @param updatedRequest Request object
     * @param func Successful callback that the download will be returned on.
     * @param func2 Failed callback that the error will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun updateRequest(requestId: Int, updatedRequest: Request, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

    /** Replaces the existing extras object associated with an existing download/request with the newly passed in extras object.
     * @param id Id of existing request/download
     * @param extras new extras object
     * @param func Successful callback that the download will be returned on.
     * @param func2 Failed callback that the error will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun replaceExtras(id: Int, extras: Extras, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

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
     * @param func2 Callback that the results will be returned on. Result maybe null.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownload(id: Int, func2: Func2<Download?>): Fetch

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

    /**
     * Gets all downloads containing the identifier.
     * @param identifier identifier.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloadsByRequestIdentifier(identifier: Long, func: Func<List<Download>>): Fetch

    /** Attaches a FetchListener to this instance of Fetch.
     * @param listener Fetch Listener
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun addListener(listener: FetchListener): Fetch

    /** Attaches a FetchListener to this instance of Fetch.
     * @param listener Fetch Listener
     * @param notify Allows Fetch to notify the newly attached listener instantly of the download status
     * of all downloads managed by the namespace. Default is false.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun addListener(listener: FetchListener, notify: Boolean = DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED): Fetch


    /** Attaches a FetchListener to this instance of Fetch.
     * @param listener Fetch Listener
     * @param notify Allows Fetch to notify the newly attached listener instantly of the download status
     * of all downloads managed by the namespace. Default is false.
     * @param autoStart Allows Fetch to start processing requests if it is not already doing so.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun addListener(listener: FetchListener, notify: Boolean = DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED, autoStart: Boolean): Fetch

    /** Detaches a FetchListener from this instance of Fetch.
     * @param listener Fetch Listener
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeListener(listener: FetchListener): Fetch

    /**
     * Adds a completed download to Fetch for management. If Fetch is already managing another download with the same file as this completed download's
     * file, Fetch will replace the already managed download with this completed download.
     * @param completedDownload Completed Download
     * @param alertListeners boolean indicating whether to alert all listeners attached to this fetch's namespace of the downloads completed status.
     * @param func Callback that the added download will be returned on.
     * @param func2 Callback that is called when adding the completed download fails. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun addCompletedDownload(completedDownload: CompletedDownload, alertListeners: Boolean = true, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Adds a list of completed downloads to Fetch for management. If Fetch is already managing another download with the same file as this completed download's
     * file, Fetch will replace the already managed download with this completed download.
     * @param completedDownloads Completed Downloads list
     * @param alertListeners boolean indicating whether to alert all listeners attached to this fetch's namespace of the downloads completed status.
     * @param func Callback that the added downloads list will be returned on.
     * @param func2 Callback that is called when adding the completed downloads fails. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun addCompletedDownloads(completedDownloads: List<CompletedDownload>, alertListeners: Boolean = true, func: Func<List<Download>>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Gets the list of download blocks belonging to a download. List may be empty if
     * blocks could not be found for the download id or download has never been processed.
     * @param downloadId: Download ID
     * @param func Callback the results will be returned on
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloadBlocks(downloadId: Int, func: Func<List<DownloadBlock>>): Fetch

    /**
     * Gets the content Length for a request. If the request or contentLength cannot be found in
     * the Fetch database(meaning Fetch never processed the request and started downloading it) -1 is returned.
     * However, setting fromServer to true will create a new connection to the server to get the connectLength
     * if Fetch does not already contain the data in the database for the request.
     * @param request Request. Can be a managed or un-managed request. The request is not stored in
     * the fetch database.
     * @param fromServer If true, fetch will attempt to get the ContentLength
     * from the server directly by making a network request. Otherwise no action is taken.
     * @param func callback result will be returned on. If the result is -1. This indicates that
     * Fetch was not able to get the ContentLength.
     * @param func2 where the error will be returned if one occurs. This indicates that
     * Fetch was not able to get the ContentLength.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getContentLengthForRequest(request: Request, fromServer: Boolean, func: Func<Long>, func2: Func<Error>?): Fetch

    /**
     * Gets the full File Resource Catalog of a Fetch File Server.
     * @param request Request. Can be a managed or un-managed request. The request is not stored in
     * the fetch database.
     * @param func callback the result is returned on.
     * @param func2 callback the error is returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getFetchFileServerCatalog(request: Request, func: Func<List<FileResource>>, func2: Func<Error>? = null): Fetch

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
    fun setDownloadConcurrentLimit(downloadConcurrentLimit: Int): Fetch

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
        @Volatile
        private var defaultFetchConfiguration: FetchConfiguration? = null
        @Volatile
        private var defaultFetchInstance: Fetch? = null

        /**
         * Sets the default Configuration settings on the default Fetch instance.
         * @param fetchConfiguration custom Fetch Configuration
         * */
        fun setDefaultInstanceConfiguration(fetchConfiguration: FetchConfiguration) {
            synchronized(lock) {
                defaultFetchConfiguration = fetchConfiguration
            }
        }

        /**
         * Get the default Fetch Configuration set with setDefaultInstanceConfiguration(fetchConfiguration: FetchConfiguration)
         * or setDefaultInstanceConfiguration(context: Context)
         * @return default FetchConfiguration
         * */
        fun getDefaultFetchConfiguration(): FetchConfiguration? {
            return synchronized(lock) {
                defaultFetchConfiguration
            }
        }

        /**
         * @throws FetchException if default FetchConfiguration is not set.
         * @return Get default Fetch instance
         * */
        fun getDefaultInstance(): Fetch {
            return synchronized(lock) {
                val fetchConfiguration = defaultFetchConfiguration
                        ?: throw FetchException(GLOBAL_FETCH_CONFIGURATION_NOT_SET)
                val defaultFetch = defaultFetchInstance
                if (defaultFetch == null || defaultFetch.isClosed) {
                    val newDefaultFetch = FetchImpl.newInstance(FetchModulesBuilder.buildModulesFromPrefs(fetchConfiguration))
                    defaultFetchInstance = newDefaultFetch
                    newDefaultFetch
                } else {
                    defaultFetch
                }
            }
        }

        /**
         * Creates a custom Instance of Fetch with the given configuration and namespace.
         * @param fetchConfiguration custom Fetch Configuration
         * @return custom Fetch instance
         * */
        fun getInstance(fetchConfiguration: FetchConfiguration): Fetch {
            return FetchImpl.newInstance(FetchModulesBuilder.buildModulesFromPrefs(fetchConfiguration))
        }

    }

}
