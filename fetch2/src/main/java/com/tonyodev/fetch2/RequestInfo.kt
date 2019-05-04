package com.tonyodev.fetch2

import com.tonyodev.fetch2.util.*
import com.tonyodev.fetch2core.Extras
import java.io.Serializable

/**
 * A RequestInfo allows you to update an existing download managed by Fetch.
 * Note: The fields in this class will overwrite the corresponding fields for an
 * existing download. Be sure to update all the fields in this class with
 * the proper values.
 * */
open class RequestInfo : Serializable {

    /** Can be used to set your own unique identifier for the request.*/
    var identifier: Long = DEFAULT_UNIQUE_IDENTIFIER

    /** The group id this download belongs to.*/
    var groupId: Int = DEFAULT_GROUP_ID

    /** The headers used by the downloader to send header information to
     * the server about a request.*/
    val headers: MutableMap<String, String> = mutableMapOf()

    /** The download Priority of this download.
     * @see com.tonyodev.fetch2.Priority */
    var priority: Priority = defaultPriority

    /** The network type this download is allowed to download on.
     * @see com.tonyodev.fetch2.NetworkType*/
    var networkType: NetworkType = defaultNetworkType

    /** Adds a header for the download.
     * @param key Header Key
     * @param value Header Value
     * */
    fun addHeader(key: String, value: String) {
        this.headers[key] = value
    }

    /** Associate a tag for this request*/
    var tag: String? = null

    /**
     * Action used by Fetch when enqueuing a request and a previous request with the
     * same file is already being managed. Default EnqueueAction.REPLACE_EXISTING
     * which will replaces the existing request.
     * */
    var enqueueAction = defaultEnqueueAction

    /**
     * Action used by Fetch when enqueuing a request to determine if to place the new request in
     * the downloading queue immediately after enqueue to be processed with its turn arrives
     * The default value is true.
     * If true, the download will have a status of Status.QUEUED. If false, the download will have a status
     * of Status.ADDED.
     * */
    var downloadOnEnqueue = DEFAULT_DOWNLOAD_ON_ENQUEUE

    /**
     * The maximum number of times Fetch will auto retry a failed download.
     * The default is 0. Value has to be greater than -1
     * @throws IllegalArgumentException if value passed in is less than 0
     * */
    var autoRetryMaxAttempts = DEFAULT_AUTO_RETRY_ATTEMPTS
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("The maximum number of attempts has to be greater than -1")
            }
            field = value
        }

    /**
     * Set or get the extras for this request. Use this to
     * save and get custom key/value data for the request.
     * Use fetch.replaceExtras(id, extras)
     * */
    var extras: Extras = Extras.emptyExtras
        set(value) {
            field = value.copy()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RequestInfo
        if (identifier != other.identifier) return false
        if (groupId != other.groupId) return false
        if (headers != other.headers) return false
        if (priority != other.priority) return false
        if (networkType != other.networkType) return false
        if (tag != other.tag) return false
        if (enqueueAction != other.enqueueAction) return false
        if (downloadOnEnqueue != other.downloadOnEnqueue) return false
        if (extras != other.extras) return false
        if (autoRetryMaxAttempts != other.autoRetryMaxAttempts) return false
        return true
    }

    override fun hashCode(): Int {
        var result = identifier.hashCode()
        result = 31 * result + groupId
        result = 31 * result + headers.hashCode()
        result = 31 * result + priority.hashCode()
        result = 31 * result + networkType.hashCode()
        result = 31 * result + (tag?.hashCode() ?: 0)
        result = 31 * result + enqueueAction.hashCode()
        result = 31 * result + downloadOnEnqueue.hashCode()
        result = 31 * result + extras.hashCode()
        result = 31 * result + autoRetryMaxAttempts
        return result
    }

    override fun toString(): String {
        return "RequestInfo(identifier=$identifier, groupId=$groupId," +
                " headers=$headers, priority=$priority, networkType=$networkType," +
                " tag=$tag, enqueueAction=$enqueueAction, downloadOnEnqueue=$downloadOnEnqueue, " +
                "autoRetryMaxAttempts=$autoRetryMaxAttempts, extras=$extras)"
    }

}