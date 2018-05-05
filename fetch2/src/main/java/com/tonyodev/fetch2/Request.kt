package com.tonyodev.fetch2

/**
 * Use this class to create a request that is used by Fetch to enqueue a download and
 * begin the download process.
 * */
open class Request constructor(
        /** Used to identify a download. Must be UNIQUE.*/
        val id: Int,

        /** The url where the file will be downloaded from.*/
        val url: String,

        /** The file eg(/files/download.txt) where the file will be
         * downloaded to and saved on disk.*/
        val file: String) : RequestInfo() {

    constructor(
            /** The url where the file will be downloaded from.*/
            url: String,

            /** The file eg(/files/download.txt) where the file will be
             * downloaded to and saved on disk.*/
            file: String) : this((url.hashCode() * 31) + file.hashCode(), url, file)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Request
        if (url != other.url) return false
        if (file != other.file) return false
        if (id != other.id) return false
        if (groupId != other.groupId) return false
        if (headers != other.headers) return false
        if (priority != other.priority) return false
        if (networkType != other.networkType) return false
        if (tag != other.tag) return false
        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + id
        result = 31 * result + groupId
        result = 31 * result + headers.hashCode()
        result = 31 * result + priority.hashCode()
        result = 31 * result + networkType.hashCode()
        result = 31 * result + (tag?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Request(url='$url', file='$file', id=$id, groupId=$groupId, " +
                "headers=$headers, priority=$priority, networkType=$networkType, tag=$tag)"
    }

}