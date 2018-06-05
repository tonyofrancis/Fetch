package com.tonyodev.fetch2fileserver.database

import android.arch.persistence.room.*
import com.tonyodev.fetch2fileserver.ContentFile
import com.tonyodev.fetch2fileserver.database.ContentFileDatabase.Companion.COLUMN_ID
import com.tonyodev.fetch2fileserver.database.ContentFileDatabase.Companion.COLUMN_NAME
import com.tonyodev.fetch2fileserver.database.ContentFileDatabase.Companion.TABLE_NAME

@Dao
interface ContentFileDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(contentFile: ContentFile): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(contentFileList: List<ContentFile>): List<Long>

    @Delete
    fun delete(contentFile: ContentFile)

    @Delete
    fun delete(contentFileList: List<ContentFile>)

    @Query("DELETE FROM $TABLE_NAME")
    fun deleteAll()

    @Query("SELECT * FROM $TABLE_NAME")
    fun get(): List<ContentFile>

    @Query("SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID = :id")
    fun get(id: Long): ContentFile?

    @Query("SELECT * FROM $TABLE_NAME WHERE $COLUMN_NAME = :fileName LIMIT 1")
    fun get(fileName: String): ContentFile?

    @Query("SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID IN (:ids)")
    fun get(ids: List<Long>): List<ContentFile>

    @Query("SELECT * FROM $TABLE_NAME LIMIT :count OFFSET :offset")
    fun getPage(count: Int, offset: Int): List<ContentFile>

}