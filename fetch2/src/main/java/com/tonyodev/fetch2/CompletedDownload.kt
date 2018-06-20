package com.tonyodev.fetch2

import java.util.*

/** Class used to enqueue an already completed download into Fetch for management.*/
open class CompletedDownload {

    /** The url where the file was downloaded from.*/
    var url: String = ""

    /** The downloaded file eg(/files/download.txt).*/
    var file: String = ""

    /** The group id this download belongs to.*/
    var group: Int = 0

    /** The file size of the download in bytes.*/
    var fileByteSize: Long = 0

    /** The download request header information.*/
    var headers: Map<String, String> = emptyMap()

    /** The tag associated with this download.*/
    var tag: String? = null

    /** Set your own unique identifier for the download.*/
    var identifier: Long = 0

    /** The timestamp when this download was created.*/
    var created: Long = Date().time

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CompletedDownload
        if (url != other.url) return false
        if (file != other.file) return false
        if (group != other.group) return false
        if (headers != other.headers) return false
        if (tag != other.tag) return false
        if (identifier != other.identifier) return false
        if (created != other.created) return false
        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + group
        result = 31 * result + headers.hashCode()
        result = 31 * result + (tag?.hashCode() ?: 0)
        result = 31 * result + identifier.hashCode()
        result = 31 * result + created.hashCode()
        return result
    }

    override fun toString(): String {
        return "CompletedDownload(url='$url', file='$file', group=$group, " +
                "headers=$headers, tag=$tag, identifier=$identifier, created=$created)"
    }

}