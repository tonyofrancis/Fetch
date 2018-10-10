package com.tonyodev.fetch2

import android.os.Parcelable
import com.tonyodev.fetch2core.Extras

/**
 * An immutable object which contains a current snapshot of all the information
 * about a specific download managed by Fetch.
 * */
interface Download : Parcelable {

    /** Used to identify a download. This id also matches the id of the request that started
     * the download.*/
    val id: Int

    /** The Fetch namespace this download belongs to.*/
    val namespace: String

    /** The url where the file will be downloaded from.*/
    val url: String

    /** The file eg(/files/download.txt) where the file will be
     * downloaded to and saved on disk.*/
    val file: String

    /** The group id this download belongs to.*/
    val group: Int

    /** The download Priority of this download.
     * @see com.tonyodev.fetch2.Priority
     * */
    val priority: Priority

    /** The headers used by the downloader to send header information to
     * the server about a request.*/
    val headers: Map<String, String>

    /** The amount of bytes downloaded thus far and saved to the file.*/
    val downloaded: Long

    /** The file size of a download in bytes. This field could return -1 if the server
     * did not readily provide the Content-Length when the connection was established.*/
    val total: Long

    /** The current status of a download.
     *  @see com.tonyodev.fetch2.Status
     *  */
    val status: Status

    /** If the download encountered an error, the download status will be Status.Failed and
     *  this field will provide the specific error when possible.
     *  Otherwise the default non-error value is Error.NONE.
     *  @see com.tonyodev.fetch2.Error
     *  */
    val error: Error

    /** The network type this download is allowed to download on.
     * @see com.tonyodev.fetch2.NetworkType
     * */
    val networkType: NetworkType

    /** The download progress thus far eg(95 indicating 95% completed). If the total field of
     * this object has a value of -1, this field will also return -1 indicating that the server
     * did not readily provide the Content-Length and that the progress is undetermined.*/
    val progress: Int

    /** The timestamp when this download was created.*/
    val created: Long

    /** The request information used to create this download.*/
    val request: Request

    /** Gets a copy of this instance. */
    fun copy(): Download

    /** Gets the tag associated with this download.*/
    val tag: String?

    /**
     * Action used by Fetch when enqueuing a request and a previous request with the
     * same file is already being managed. Default EnqueueAction.REPLACE_EXISTING
     * which will replaces the existing request.
     * */
    val enqueueAction: EnqueueAction

    /** Can be used to set your own unique identifier for the request.*/
    val identifier: Long

    /**
     * Action used by Fetch when enqueuing a request to determine if to place the new request in
     * the downloading queue immediately after enqueue to be processed with its turn arrives
     * The default value is true.
     * If true, the download will have a status of Status.QUEUED. If false, the download will have a status
     * of Status.ADDED.
     * */
    val downloadOnEnqueue: Boolean

    /** Stored custom data/ key value pairs with a request.
     * Use fetch.replaceExtras(id, extras)
     * */
    val extras: Extras

}