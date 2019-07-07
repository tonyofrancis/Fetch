package com.tonyodev.fetch2

import android.annotation.SuppressLint
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchImpl
import com.tonyodev.fetch2.fetch.FetchModulesBuilder
import com.tonyodev.fetch2.util.DEFAULT_AUTO_RETRY_ATTEMPTS
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_NOTIFY_ON_ATTACHED
import com.tonyodev.fetch2.util.DEFAULT_ENABLE_LISTENER_NOTIFY_ON_REQUEST_UPDATED
import com.tonyodev.fetch2core.*

/**
 * A light weight file download manager for Android.
 * Features: Background downloading,
 *           Queue based Priority downloading,
 *           Pause & Resume downloads,
 *           Network specific downloading and more...
 * @see <a href="https://github.com/tonyofrancis/Fetch</a>
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

    /**
     * Queues a request for downloading. If Fetch fails to enqueue the request,
     * func2 will be called with the error.
     * Errors that may cause Fetch to fail the enqueue are :
     * 1. No storage space on the device.
     * 2. Fetch is already managing the same request. This means that a request with the same url
     * and file name is already managed.
     * 3. Fetch is already managing a request that is downloading to the request file.
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
     * 3. Fetch is already managing a request that is downloading to the request file.
     * @param requests Request List
     * @param func Callback that the enqueued or failed requests will be returned on.
     *             This callback returns a list with a pair<Request,Error> for each enqueued request.
     *             If the pair's second parameter is Error.NONE. this indicates that the request
     *             was enqueued successfully. If the Error is not ERROR.NONE. This indicates
     *             that the request was not enqueued for the specified reason.
     *             Fetch may update a request depending on the initial request's Enqueue Action.
     *             Update old request references with this request.
     *
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun enqueue(requests: List<Request>, func: Func<List<Pair<Request, Error>>>? = null): Fetch

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

    /**
     * Pause all downloads associated by this fetch.
     *
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun pauseAll(): Fetch

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
     * Resume all paused downloads associated the specified group.
     *
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun resumeAll(): Fetch

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
     * @param statuses statuses
     * @param func callback returning a list of downloads that were removed.
     * @param func2 Callback that is called when attempting to remove downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeAllInGroupWithStatus(id: Int, statuses: List<Status>, func: Func<List<Download>>?, func2: Func<Error>? = null): Fetch

    /**
     * Remove all downloads with the specified group and status in this instance of Fetch.
     * The downloaded files for removed downloads are not deleted.
     * @param statuses statuses
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun removeAllInGroupWithStatus(id: Int, statuses: List<Status>): Fetch

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
     * @param statuses statuses
     * @param func callback returns all deleted downloads with a specified status.
     * @param func2 Callback that is called when attempting to delete downloads fail. An error is returned.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteAllInGroupWithStatus(id: Int, statuses: List<Status>, func: Func<List<Download>>?, func2: Func<Error>? = null): Fetch

    /**
     * Deletes all downloads with the specified group and status in this instance of Fetch.
     * The downloaded files are also deleted.
     * @param statuses statuses
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun deleteAllInGroupWithStatus(id: Int, statuses: List<Status>): Fetch

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
     * @param notifyListeners If the request is successfully updated notify attached Fetch listeners of the download status. Default true
     * @param func Successful callback that the download will be returned on.
     * @param func2 Failed callback that the error will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun updateRequest(requestId: Int,
                      updatedRequest: Request,
                      notifyListeners: Boolean = DEFAULT_ENABLE_LISTENER_NOTIFY_ON_REQUEST_UPDATED,
                      func: Func<Download>? = null,
                      func2: Func<Error>? = null): Fetch

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
     * Resets the autoRetryAttempts value for a download back to 0.
     * @param downloadId Id of existing request/download
     * @param retryDownload Retry the download if its status is Status.ERROR. True by default.
     * @param func callback that returns the download if it exists.
     * @param func2 callback that returns the error if on occurred.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun resetAutoRetryAttempts(downloadId: Int, retryDownload: Boolean = true, func: Func2<Download?>? = null, func2: Func<Error>? = null): Fetch

    /**
     * Renames the file for a completed download. The StorageResolver attached to this fetch instance will rename the file.
     * So it is okay to parse uri strings for the newFileName.
     * @param id Id of existing request/download
     * @param newFileName the new file name.
     * @param func Successful callback that the download will be returned on.
     * @param func2 Failed callback that the error will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun renameCompletedDownloadFile(id: Int, newFileName: String, func: Func<Download>? = null, func2: Func<Error>? = null): Fetch

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
     * Gets all downloads with a specific status.
     * @see com.tonyodev.fetch2.Status
     * @param statuses Statuses to query.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloadsWithStatus(statuses: List<Status>, func: Func<List<Download>>): Fetch

    /**
     * Gets all downloads in a specific group with a specific status.
     * @see com.tonyodev.fetch2.Status
     * @param groupId group id to query.
     * @param statuses Statuses to query.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloadsInGroupWithStatus(groupId: Int, statuses: List<Status>, func: Func<List<Download>>): Fetch

    /**
     * Gets all downloads containing the identifier.
     * @param identifier identifier.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloadsByRequestIdentifier(identifier: Long, func: Func<List<Download>>): Fetch

    /**
     * Gets all downloads containing the tag.
     * @param tag tag.
     * @param func Callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getDownloadsByTag(tag: String, func: Func<List<Download>>): Fetch

    /**
     * Gets the FetchGroup by id. Even if the database does not contain downloads with this group id
     * a FetchGroup will be returned. It will contain no downloads however. When a download with this
     * group id is added. The downloads field on this object will be update and attached FetchObservers will be notified.
     * @param group the group id
     * @param func callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance.
     * */
    fun getFetchGroup(group: Int, func: Func<FetchGroup>): Fetch

    /**
     * Gets a list of the ids of all groups managed my this fetch namespace.
     * @param func callback that the results will be returned on.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance.
     * */
    fun getAllGroupIds(func: Func<List<Int>>): Fetch

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
     * Gets the content Length for each request in the passed in list. If the request or contentLength cannot be found in
     * the Fetch database(meaning Fetch never processed the request and started downloading it) -1 is returned.
     * However, setting fromServer to true will create a new connection to the server to get the connectLength
     * if Fetch does not already contain the data in the database for the request.
     * @param requests Request list. Can be a managed or un-managed list of requests. The requests are not stored in
     * the fetch database.
     * @param fromServer If true, fetch will attempt to get the ContentLength
     * from the server directly by making a network request. Otherwise no action is taken.
     * @param func callback which returns a list of all the success request pairs with their content length.
     * @param func2 callback used to return a list of all the request that error when trying to get their content length.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getContentLengthForRequests(requests:List<Request>, fromServer: Boolean, func: Func<List<Pair<Request,Long>>>, func2: Func<List<Pair<Request, Error>>>): Fetch

    /**
     * Gets the Server Response for the url and associated headers.
     * @param url the url. Cannot be null.
     * @param headers the request headers for the url. Can be null.
     * @param func the callback the server response is returned on. Cannot be null.
     * @param func2 the callback that is executed with an error occurs. Can be null.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return Instance
     * */
    fun getServerResponse(url: String, headers: Map<String, String>?, func: Func<Downloader.Response>, func2: Func<Error>? = null): Fetch

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
     * */
    fun close()

    /** Gets the set of FetchListeners attached to this instance.
     * @return set of attached FetchListeners.
     * */
    fun getListenerSet(): Set<FetchListener>

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
    fun attachFetchObserversForDownload(downloadId: Int, vararg fetchObservers: FetchObserver<Download>): Fetch

    /**
     * Removes a FetchObserver attached to this Fetch namespace for a download.
     * @param downloadId the download Id
     * @param fetchObservers the fetch observers
     * @throws FetchException if this instance of Fetch has been closed.
     * @return instance
     * */
    fun removeFetchObserversForDownload(downloadId: Int, vararg fetchObservers: FetchObserver<Download>): Fetch

    /** Indicates if this fetch namespace has active(Queued or Downloading) downloads. You can use this value to
     * keep a background service ongoing until the callback function returns false.
     * @param includeAddedDownloads To include downloads with a status of Added. Added downloads are not considered active by default.
     * @param func the callback function
     * @throws FetchException if accessed on ui thread
     * @return instance
     * */
    fun hasActiveDownloads(includeAddedDownloads: Boolean, func: Func<Boolean>): Fetch

    /** Subscribe a FetchObserver that indicates if this fetch namespace has active(Queued or Downloading) downloads. You can use this value to
     * keep a background service ongoing until the value returned is false.
     * @param includeAddedDownloads To include downloads with a status of Added. Added downloads are not considered active by default.
     * @param fetchObserver the fetch observer
     * @throws FetchException if this instance of Fetch has been closed.
     * @return instance
     * */
    fun addActiveDownloadsObserver(includeAddedDownloads: Boolean = false, fetchObserver: FetchObserver<Boolean>): Fetch

    /** Removes a subscribed FetchObserver that is listening for active downloads.
     * @param fetchObserver the fetch observer to remove.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return instance
     * */
    fun removeActiveDownloadsObserver(fetchObserver: FetchObserver<Boolean>): Fetch

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
