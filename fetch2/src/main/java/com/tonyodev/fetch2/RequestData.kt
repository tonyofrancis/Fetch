package com.tonyodev.fetch2

import android.support.v4.util.ArrayMap

class RequestData(val url: String, val absoluteFilePath: String, status: Int,
                  error: Int, val downloadedBytes: Long, val totalBytes: Long, headers: MutableMap<String, String>, val groupId: String) {
    val status: Status
    val error: Error
    val progress: Int
    val headers: Map<String, String>
    val request: Request

    val id: Long
        get() = request.id

    init {
        var headers = headers

        if (url == null) {
            throw IllegalArgumentException("Url cannot be null")
        }

        if (absoluteFilePath == null) {
            throw IllegalArgumentException("AbsoluteFilePath cannot be null")
        }

        if (groupId == null) {
            throw IllegalArgumentException("groupId cannot be null")
        }

        if (headers == null) {
            headers = ArrayMap()
        }
        this.error = Error.valueOf(error)
        this.status = Status.valueOf(status)
        this.progress = DownloadHelper.calculateProgress(downloadedBytes, totalBytes)
        this.headers = headers
        this.request = Request(url, absoluteFilePath, headers)
    }

    override fun toString(): String {
        return request.toString()
    }
}
