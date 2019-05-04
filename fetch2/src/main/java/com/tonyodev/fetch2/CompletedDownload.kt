package com.tonyodev.fetch2

import android.os.Parcel
import android.os.Parcelable
import com.tonyodev.fetch2core.Extras
import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

/** Class used to enqueue an already completed download into Fetch for management.*/
open class CompletedDownload : Parcelable, Serializable {

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
    var created: Long = Calendar.getInstance().timeInMillis

    /**
     * Set or get the extras for this request. Use this to
     * save and get custom key/value data for the request.
     * */
    var extras: Extras = Extras.emptyExtras

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
        if (extras != other.extras) return false
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
        result = 31 * result + extras.hashCode()
        return result
    }

    override fun toString(): String {
        return "CompletedDownload(url='$url', file='$file', groupId=$group, " +
                "headers=$headers, tag=$tag, identifier=$identifier, created=$created, " +
                "extras=$extras)"
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(url)
        dest.writeString(file)
        dest.writeInt(group)
        dest.writeLong(fileByteSize)
        dest.writeSerializable(HashMap(headers))
        dest.writeString(tag)
        dest.writeLong(identifier)
        dest.writeLong(created)
        dest.writeSerializable(HashMap(extras.map))
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CompletedDownload> {

        @Suppress("UNCHECKED_CAST")
        override fun createFromParcel(source: Parcel): CompletedDownload {
            val url = source.readString() ?: ""
            val file = source.readString() ?: ""
            val groupId = source.readInt()
            val fileByteSize = source.readLong()
            val headers = source.readSerializable() as Map<String, String>
            val tag = source.readString()
            val identifier = source.readLong()
            val created = source.readLong()
            val extras = source.readSerializable() as Map<String, String>

            val completedDownload = CompletedDownload()
            completedDownload.url = url
            completedDownload.file = file
            completedDownload.group = groupId
            completedDownload.fileByteSize = fileByteSize
            completedDownload.headers = headers
            completedDownload.tag = tag
            completedDownload.identifier = identifier
            completedDownload.created = created
            completedDownload.extras = Extras(extras)
            return completedDownload
        }

        override fun newArray(size: Int): Array<CompletedDownload?> {
            return arrayOfNulls(size)
        }

    }

}