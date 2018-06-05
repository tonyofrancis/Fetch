package com.tonyodev.fetch2fileserver

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import com.tonyodev.fetch2fileserver.database.ContentFileDatabase

@Entity(tableName = ContentFileDatabase.TABLE_NAME,
        indices = [(Index(value = [ContentFileDatabase.COLUMN_NAME], unique = true)),
            (Index(value = [ContentFileDatabase.COLUMN_FILE], unique = true))])
class ContentFile {

    @PrimaryKey
    @ColumnInfo(name = ContentFileDatabase.COLUMN_ID)
    var id: Long = 0L

    @ColumnInfo(name = ContentFileDatabase.COLUMN_LENGTH)
    var length: Long = 0L

    @ColumnInfo(name = ContentFileDatabase.COLUMN_FILE)
    var file: String = ""

    @ColumnInfo(name = ContentFileDatabase.COLUMN_NAME)
    var name: String = ""

    @ColumnInfo(name = ContentFileDatabase.COLUMN_CUSTOM_DATA)
    var customData: String = ""

    @ColumnInfo(name = ContentFileDatabase.COLUMN_MD5)
    var md5: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContentFile

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