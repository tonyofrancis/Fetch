package com.tonyodev.fetch2

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

@Dao
interface RequestInfoDao {

    @Insert
    fun insert(requestInfo: RequestInfo): Long

    @Insert
    fun insert(requestInfoList: List<RequestInfo>): List<Long>

    @Query("SELECT * FROM requestInfos WHERE status = :status")
    fun queryByStatus(status: Int): List<RequestInfo>

    @Query("SELECT * FROM requestInfos WHERE groupId = :groupId")
    fun queryByGroupId(groupId: String): List<RequestInfo>

    @Query("SELECT * FROM requestInfos WHERE id = :id LIMIT 1")
    fun query(id: Long): RequestInfo?

    @Query("SELECT * FROM requestInfos")
    fun query(): List<RequestInfo>

    @Query("SELECT * FROM requestInfos WHERE id IN(:ids)")
    fun query(ids: LongArray): List<RequestInfo>

    @Query("UPDATE requestInfos SET downloadedBytes = :downloadedBytes WHERE id = :id")
    fun updateDownloadedBytes(id: Long, downloadedBytes: Long)

    @Query("UPDATE requestInfos SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes WHERE id = :id")
    fun setDownloadedBytesAndTotalBytes(id: Long, downloadedBytes: Long, totalBytes: Long)

    @Query("UPDATE requestInfos SET status = :status, error = :error WHERE id = :id")
    fun setStatusAndError(id: Long, status: Int, error: Int)

    @Query("DELETE FROM requestInfos WHERE id = :id")
    fun remove(id: Long)

    @Query("DELETE FROM requestInfos")
    fun deleteAll()
}
