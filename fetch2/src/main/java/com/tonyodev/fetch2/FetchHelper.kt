package com.tonyodev.fetch2

import android.content.Context

import okhttp3.OkHttpClient

internal object FetchHelper {

    val defaultDatabaseName: String
        get() = "com_tonyodev_fetch"

    fun throwIfContextIsNull(context: Context?) {

        if (context == null) {
            throw IllegalArgumentException("Context cannot be null")
        }
    }


    fun throwIfFetchNameIsNullOrEmpty(databaseName: String?) {

        if (databaseName == null || databaseName.isEmpty()) {
            throw IllegalArgumentException("DatabaseManager Name cannot be null or empty")
        }
    }

    fun throwIfGroupIDIsNull(groupId: String?) {

        if (groupId == null) {
            throw IllegalArgumentException("groupId cannot be null")
        }
    }

    fun throwIfRequestIsNull(request: Request?) {

        if (request == null) {
            throw IllegalArgumentException("Request cannot be null")
        }
    }

    fun throwIfClientIsNull(client: OkHttpClient?) {

        if (client == null) {
            throw IllegalArgumentException("OkHttpClient cannot be null")
        }
    }

    fun throwIfStatusIsNull(status: Status?) {

        if (status == null) {
            throw IllegalArgumentException("Status cannot be null")
        }
    }

    fun throwIfRequestListIsNull(list: List<Request>?) {

        if (list == null) {
            throw IllegalArgumentException("List<Request> cannot be null")
        }
    }

    fun throwIfIdListIsNull(list: List<Long>?) {

        if (list == null) {
            throw IllegalArgumentException("List<Long> cannot be null")
        }
    }

    fun throwIfCallbackIsNull(callback: Callback?) {

        if (callback == null) {
            throw IllegalArgumentException("Callback cannot be null")
        }
    }

    fun throwIfQueryIsNull(query: Query<*>?) {

        if (query == null) {
            throw IllegalArgumentException("Query cannot be null")
        }
    }

    fun throwIfDisposed(disposable: Disposable) {

        if (disposable.isDisposed) {
            throw DisposedException("This instance cannot be reused after disposed is called")
        }
    }

    fun createIdArray(ids: List<Long>): LongArray {

        for (id in ids) {
            if (id == null) {
                throw NullPointerException("id inside List<Long> cannot be null")
            }
        }

        val idArray = LongArray(ids.size)
        for (i in ids.indices) {
            idArray[i] = ids[i]
        }

        return idArray
    }
}
