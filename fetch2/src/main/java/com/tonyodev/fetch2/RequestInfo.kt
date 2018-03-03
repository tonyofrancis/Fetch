package com.tonyodev.fetch2

import com.tonyodev.fetch2.util.DEFAULT_GROUP_ID
import com.tonyodev.fetch2.util.defaultNetworkType
import com.tonyodev.fetch2.util.defaultPriority

/**
 * A RequestInfo allows you to update an existing download managed by Fetch.
 * Note: The fields in this class will overwrite the corresponding fields for an
 * existing download. Be sure to update all the fields in this class with
 * the proper values.
 * */
open class RequestInfo {

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

    /** Adds a header for a download.
     * @param key Header Key
     * @param value Header Value
     * */
    fun addHeader(key: String, value: String) {
        this.headers[key] = value
    }

    /** Associate a tag for this request*/
    var tag: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RequestInfo

        if (groupId != other.groupId) return false
        if (headers != other.headers) return false
        if (priority != other.priority) return false
        if (networkType != other.networkType) return false
        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId
        result = 31 * result + headers.hashCode()
        result = 31 * result + priority.hashCode()
        result = 31 * result + networkType.hashCode()
        result = 31 * result + (tag?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "RequestInfo(groupId=$groupId, headers=$headers, priority=$priority, " +
                "networkType=$networkType, tag=$tag)"
    }

}