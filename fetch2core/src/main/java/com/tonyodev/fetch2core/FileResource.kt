package com.tonyodev.fetch2core

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

/** File Resource used by Fetch File Server to server content data.
 * */
class FileResource : Parcelable, Serializable {

    /** Unique File Resource Identifier.
     * Clients can request data from the server using the file resource id
     * example Url: fetchlocal://127.0.0.1:7428/84562
     * */
    var id: Long = 0L

    /** Content Length */
    var length: Long = 0L

    /** Absolute File Path */
    var file: String = ""

    /** Unique Short name of the File Resource.
     * Clients can request data from the server using the file resource name
     * example Url: fetchlocal://127.0.0.1/:7428/text.txt
     * */
    var name: String = ""

    /**
     * Set or get the extras for this file resource. Use this to
     * save and get custom key/value data for the file resource.
     * */
    var extras: Extras = Extras.emptyExtras
        set(value) {
            field = value.copy()
        }

    /** The File Resource md5 checksum string. If Empty Fetch File Server will generate the md5
     * checksum when added to a file server instance.
     * This is sent in the server response to the client.*/
    var md5: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileResource
        if (id != other.id) return false
        if (length != other.length) return false
        if (file != other.file) return false
        if (name != other.name) return false
        if (extras != other.extras) return false
        if (md5 != other.md5) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + length.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + extras.hashCode()
        result = 31 * result + md5.hashCode()
        return result
    }

    override fun toString(): String {
        return "FileResource(id=$id, length=$length, file='$file'," +
                " name='$name', extras='$extras', md5='$md5')"
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(name)
        dest.writeLong(length)
        dest.writeString(file)
        dest.writeSerializable(HashMap(extras.map))
        dest.writeString(md5)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FileResource> {

        @Suppress("UNCHECKED_CAST")
        override fun createFromParcel(source: Parcel): FileResource {
            val fileResource = FileResource()
            fileResource.id = source.readLong()
            fileResource.name = source.readString() ?: ""
            fileResource.length = source.readLong()
            fileResource.file = source.readString() ?: ""
            fileResource.extras = Extras(source.readSerializable() as HashMap<String, String>)
            fileResource.md5 = source.readString() ?: ""
            return fileResource
        }

        override fun newArray(size: Int): Array<FileResource?> {
            return arrayOfNulls(size)
        }
    }

}