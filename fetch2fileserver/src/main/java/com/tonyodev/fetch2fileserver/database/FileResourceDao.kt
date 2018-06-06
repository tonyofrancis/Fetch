package com.tonyodev.fetch2fileserver.database

import android.arch.persistence.room.*
import com.tonyodev.fetch2fileserver.FileResource
import com.tonyodev.fetch2fileserver.database.FileResourceDatabase.Companion.COLUMN_ID
import com.tonyodev.fetch2fileserver.database.FileResourceDatabase.Companion.COLUMN_NAME
import com.tonyodev.fetch2fileserver.database.FileResourceDatabase.Companion.TABLE_NAME

@Dao
interface FileResourceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(fileResource: FileResource): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(fileResourceList: List<FileResource>): List<Long>

    @Delete
    fun delete(fileResource: FileResource)

    @Delete
    fun delete(fileResourceList: List<FileResource>)

    @Query("DELETE FROM $TABLE_NAME")
    fun deleteAll()

    @Query("SELECT * FROM $TABLE_NAME")
    fun get(): List<FileResource>

    @Query("SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID = :id")
    fun get(id: Long): FileResource?

    @Query("SELECT * FROM $TABLE_NAME WHERE $COLUMN_NAME = :fileName LIMIT 1")
    fun get(fileName: String): FileResource?

    @Query("SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID IN (:ids)")
    fun get(ids: List<Long>): List<FileResource>

    @Query("SELECT * FROM $TABLE_NAME LIMIT :count OFFSET :offset")
    fun getPage(count: Int, offset: Int): List<FileResource>

}