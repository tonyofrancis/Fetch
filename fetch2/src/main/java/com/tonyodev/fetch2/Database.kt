package com.tonyodev.fetch2

/**
 * Created by tonyofrancis on 6/11/17.
 */

internal interface Database {

    operator fun contains(id: Long): Boolean
    fun insert(id: Long, url: String, absoluteFilePath: String, groupId: String): Boolean
    fun queryByStatus(status: Int): List<RequestData>
    fun query(id: Long): RequestData?
    fun query(): List<RequestData>
    fun query(ids: LongArray): List<RequestData>
    fun queryByGroupId(groupId: String): List<RequestData>
    fun updateDownloadedBytes(id: Long, downloadedBytes: Long)
    fun setDownloadedBytesAndTotalBytes(id: Long, downloadedBytes: Long,
                                        totalBytes: Long)

    fun remove(id: Long)
    fun setStatusAndError(id: Long, status: Status, error: Int)
}
