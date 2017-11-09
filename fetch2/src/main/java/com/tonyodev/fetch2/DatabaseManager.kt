package com.tonyodev.fetch2

import android.arch.persistence.room.Room
import android.content.Context

import java.util.ArrayList

internal class DatabaseManager private constructor(context: Context, name: String) : Disposable {

    @Volatile override var isDisposed: Boolean = false
        private set
    private val db: FetchDatabase = Room.databaseBuilder(context,
            FetchDatabase::class.java, name + ".db").build()

    init {

        this.isDisposed = false
    }

    fun executeTransaction(transaction: Transaction) {

        try {
            transaction.onPreExecute()
            transaction.onExecute(RealmDatabase(db))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            transaction.onPostExecute()
        }
    }

    private class RealmDatabase(private val fetchDatabase: FetchDatabase) : Database {

        override fun contains(id: Long): Boolean {

            if (fetchDatabase.requestInfoDao().query(id) != null)  {
                return true
            }

            return false
        }

        override fun insert(id: Long, url: String, absoluteFilePath: String, groupId: String): Boolean {
            if (contains(id)) {
                return false
            }

            val requestInfo = RequestInfo.newInstance(id, url, absoluteFilePath, groupId)
            val inserted = fetchDatabase.requestInfoDao().insert(requestInfo)

            return inserted != -1L
        }

        override fun queryByStatus(status: Int): List<RequestData> {
            val list = ArrayList<RequestData>()

            val requestInfos = fetchDatabase.requestInfoDao().queryByStatus(status)

            requestInfos.mapTo(list) { it.toRequestData() }

            return list
        }

        override fun query(id: Long): RequestData? {
            val requestData: RequestData?
            val requestInfo = fetchDatabase.requestInfoDao().query(id)

            requestData = requestInfo?.toRequestData()

            return requestData
        }

        override fun query(): List<RequestData> {
            val list = ArrayList<RequestData>()

            val requestInfoList = fetchDatabase.requestInfoDao().query()

            requestInfoList.mapTo(list) { it.toRequestData() }

            return list
        }

        override fun query(ids: LongArray): List<RequestData> {
            val list = ArrayList<RequestData>()

            val requestInfos = fetchDatabase.requestInfoDao().query(ids)

            requestInfos.mapTo(list) { it.toRequestData() }

            return list
        }

        override fun queryByGroupId(groupId: String): List<RequestData> {

            val requestInfos = fetchDatabase.requestInfoDao().queryByGroupId(groupId)

            return requestInfos.map { it.toRequestData() }
        }

        override fun updateDownloadedBytes(id: Long, downloadedBytes: Long) {
            fetchDatabase.requestInfoDao().updateDownloadedBytes(id, downloadedBytes)
        }

        override fun setDownloadedBytesAndTotalBytes(id: Long, downloadedBytes: Long, totalBytes: Long) {
            fetchDatabase.requestInfoDao().setDownloadedBytesAndTotalBytes(id, downloadedBytes, totalBytes)
        }

        override fun remove(id: Long) {
            fetchDatabase.requestInfoDao().remove(id)
        }

        override fun setStatusAndError(id: Long, status: Status, error: Int) {
            fetchDatabase.requestInfoDao().setStatusAndError(id, status.value, error)
        }
    }

    @Synchronized override fun dispose() {
        if (!isDisposed) {
            isDisposed = true
        }
    }

    companion object {

        fun newInstance(context: Context, name: String): DatabaseManager {
            return DatabaseManager(context, name)
        }
    }
}