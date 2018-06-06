package com.tonyodev.fetch2fileserver

import android.net.Uri
import com.tonyodev.fetch2.EnqueueAction
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2.util.*

class FetchFileServerRequestBuilder {

    private var host = "00:00:00:00"
    private var port = 0
    private var authorization = ""
    private var contentIdentifier = ""
    private var file = ""
    private val headers = mutableMapOf<String, String>()
    private var tag: String? = null
    private var enqueueAction = defaultEnqueueAction
    private var networkType = defaultNetworkType
    private var priority = defaultPriority
    private var groupId = DEFAULT_GROUP_ID
    private var identifier = DEFAULT_UNIQUE_IDENTIFIER

    fun setHostAddress(hostAddress: String): FetchFileServerRequestBuilder {
        this.host = hostAddress
        return this
    }

    fun setHostPort(port: Int): FetchFileServerRequestBuilder {
        this.port = port
        return this
    }

    fun setHostInetAddress(hostAddress: String, port: Int): FetchFileServerRequestBuilder {
        this.port = port
        this.host = hostAddress
        return this
    }

    fun setAuthorization(authorization: String): FetchFileServerRequestBuilder {
        this.authorization = authorization
        return this
    }

    fun setContentIdentifier(contentIdentifier: String): FetchFileServerRequestBuilder {
        this.contentIdentifier = contentIdentifier
        return this
    }

    fun setContentIdentifier(contentFileId: Long): FetchFileServerRequestBuilder {
        this.contentIdentifier = contentFileId.toString()
        return this
    }

    fun setDownloadFile(file: String): FetchFileServerRequestBuilder {
        this.file = file
        return this
    }

    fun addHeader(key: String, value: String): FetchFileServerRequestBuilder {
        this.headers[key] = value
        return this
    }

    fun setTag(tag: String): FetchFileServerRequestBuilder {
        this.tag = tag
        return this
    }

    fun setEnqueueAction(enqueueAction: EnqueueAction): FetchFileServerRequestBuilder {
        this.enqueueAction = enqueueAction
        return this
    }

    fun setNetworkType(networkType: NetworkType): FetchFileServerRequestBuilder {
        this.networkType = networkType
        return this
    }

    fun setPriority(priority: Priority): FetchFileServerRequestBuilder {
        this.priority = priority
        return this
    }

    fun setGroupId(groupId: Int): FetchFileServerRequestBuilder {
        this.groupId = groupId
        return this
    }

    fun setUniqueIdentifier(identifier: Long): FetchFileServerRequestBuilder {
        this.identifier = identifier
        return this
    }

    fun create(): Request {
        val url = Uri.Builder()
                .scheme(FETCH_URL_SCHEME)
                .encodedAuthority("$host:$port")
                .appendPath(contentIdentifier)
                .toString()
        val request = Request(url, file)
        addHeader("Authorization", authorization)
        headers.forEach {
            request.addHeader(it.key, it.value)
        }
        request.tag = tag
        request.enqueueAction = enqueueAction
        request.networkType = networkType
        request.priority = priority
        request.groupId = groupId
        request.identifier = identifier
        return request
    }

    companion object {
        const val FETCH_URL_SCHEME = "fetchlocal"
    }

}