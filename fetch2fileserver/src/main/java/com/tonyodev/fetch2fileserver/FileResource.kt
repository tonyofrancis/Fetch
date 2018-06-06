package com.tonyodev.fetch2fileserver

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import com.tonyodev.fetch2fileserver.database.FileResourceDatabase


/** File Resource used by Fetch File Server to server content data.
 * */
@Entity(tableName = FileResourceDatabase.TABLE_NAME,
        indices = [(Index(value = [FileResourceDatabase.COLUMN_NAME], unique = true)),
            (Index(value = [FileResourceDatabase.COLUMN_FILE], unique = true))])
class FileResource {

    /** Unique File Resource Identifier. Fetch File Server will reject any new
     * File Resource if it's id matches a file resource id already managed by the
     * Fetch File Server instance.
     * Clients can request data from the server using the file resource id
     * example Url: fetchlocal://127.0.0.1/:7428/84562
     * */
    @PrimaryKey
    @ColumnInfo(name = FileResourceDatabase.COLUMN_ID)
    var id: Long = 0L

    /** Content Length */
    @ColumnInfo(name = FileResourceDatabase.COLUMN_LENGTH)
    var length: Long = 0L

    /** Absolute File Path */
    @ColumnInfo(name = FileResourceDatabase.COLUMN_FILE)
    var file: String = ""

    /** Short name of the File Resource. This Field has to be unique for each new
     * file resource being added to a Fetch file server instance.
     * Clients can request data from the server using the file resource name
     * example Url: fetchlocal://127.0.0.1/:7428/text.txt
     * */
    @ColumnInfo(name = FileResourceDatabase.COLUMN_NAME)
    var name: String = ""

    /** Custom data that will be sent in the server response to the client if available.*/
    @ColumnInfo(name = FileResourceDatabase.COLUMN_CUSTOM_DATA)
    var customData: String = ""

    /** The File Resource md5 checksum string.
     * If provided, this is sent in the server response to the client.*/
    @ColumnInfo(name = FileResourceDatabase.COLUMN_MD5)
    var md5: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileResource
        if (id != other.id) return false
        if (length != other.length) return false
        if (file != other.file) return false
        if (name != other.name) return false
        if (customData != other.customData) return false
        if (md5 != other.md5) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + length.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + customData.hashCode()
        result = 31 * result + md5.hashCode()
        return result
    }

    override fun toString(): String {
        return "ContentFile(id=$id, length=$length, file='$file', name='$name', customData='$customData', md5='$md5')"
    }

}