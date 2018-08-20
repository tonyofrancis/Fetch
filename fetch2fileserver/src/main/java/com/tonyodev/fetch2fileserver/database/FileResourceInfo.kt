package com.tonyodev.fetch2fileserver.database

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = FileResourceInfoDatabase.TABLE_NAME,
        indices = [(Index(value = [FileResourceInfoDatabase.COLUMN_NAME], unique = true)),
            (Index(value = [FileResourceInfoDatabase.COLUMN_FILE], unique = true))])
class FileResourceInfo {

    @PrimaryKey
    @ColumnInfo(name = FileResourceInfoDatabase.COLUMN_ID)
    var id: Long = 0L

    @ColumnInfo(name = FileResourceInfoDatabase.COLUMN_LENGTH)
    var length: Long = 0L

    @ColumnInfo(name = FileResourceInfoDatabase.COLUMN_FILE)
    var file: String = ""

    @ColumnInfo(name = FileResourceInfoDatabase.COLUMN_NAME)
    var name: String = ""

    @ColumnInfo(name = FileResourceInfoDatabase.COLUMN_EXTRAS)
    var extras: String = ""

    @ColumnInfo(name = FileResourceInfoDatabase.COLUMN_MD5)
    var md5: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileResourceInfo
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
        return "FileResourceInfo(id=$id, length=$length, file='$file', name='$name'," +
                " extras='$extras', md5='$md5')"
    }

}