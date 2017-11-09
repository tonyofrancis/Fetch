package com.tonyodev.fetch2

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import android.support.v4.util.ArrayMap


/**
 * Created by tonyofrancis on 6/14/17.
 */

@TypeConverters(DatabaseConverters::class)
@Entity(tableName = "requestInfos", indices = arrayOf(Index("url"), Index("status"), Index(value = "absoluteFilePath", unique = true)))
class RequestInfo {
    @PrimaryKey
    var id: Long = 0
    var url: String? = null
    var absoluteFilePath: String? = null
    var status: Int = 0
    var downloadedBytes: Long = 0
    var totalBytes: Long = 0
    var error: Int = 0
    private var headers: MutableMap<String, String>? = null
    var groupId: String? = null

    constructor() {}

    @Ignore
    constructor(id: Long, url: String, absoluteFilePath: String,
                status: Int, downloadedBytes: Long, totalBytes: Long,
                error: Int, headers: MutableMap<String, String>, groupId: String) {
        this.id = id
        this.url = url
        this.absoluteFilePath = absoluteFilePath
        this.status = status
        this.downloadedBytes = downloadedBytes
        this.totalBytes = totalBytes
        this.error = error
        this.headers = headers
        this.groupId = groupId
    }

    fun getHeaders(): Map<String, String>? {
        return headers
    }

    fun setHeaders(headers: MutableMap<String, String>) {
        this.headers = headers
    }

    @Ignore
    internal fun toRequestData(): RequestData {

        return RequestData(url!!, absoluteFilePath!!, status, error, downloadedBytes, totalBytes, headers!!, groupId!!)
    }

    companion object {

        @Ignore
        internal fun newInstance(id: Long, url: String, absoluteFilePath: String, groupId: String): RequestInfo {

            val requestInfo = RequestInfo()
            requestInfo.id = id
            requestInfo.url = url
            requestInfo.absoluteFilePath = absoluteFilePath
            requestInfo.status = Status.QUEUED.value
            requestInfo.totalBytes = 0L
            requestInfo.downloadedBytes = 0L
            requestInfo.error = Error.NONE.value
            requestInfo.setHeaders(ArrayMap())
            requestInfo.groupId = groupId

            return requestInfo
        }
    }
}
