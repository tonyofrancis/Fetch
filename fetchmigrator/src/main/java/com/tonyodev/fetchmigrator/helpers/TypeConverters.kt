package com.tonyodev.fetchmigrator.helpers

import android.database.Cursor
import com.tonyodev.fetch2.EnqueueAction
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.DownloadInfo
import com.tonyodev.fetch2.database.FetchDatabaseManagerWrapper
import com.tonyodev.fetch2.util.DEFAULT_DOWNLOAD_ON_ENQUEUE
import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetchmigrator.fetch1.DatabaseHelper
import com.tonyodev.fetchmigrator.fetch1.DownloadTransferPair
import com.tonyodev.fetchmigrator.fetch1.FetchConst
import org.json.JSONObject

fun v1CursorToV2DownloadInfo(cursor: Cursor, databaseManagerWrapper: FetchDatabaseManagerWrapper): DownloadTransferPair {
    val id = cursor.getLong(DatabaseHelper.INDEX_ID)
    val status = cursor.getInt(DatabaseHelper.INDEX_COLUMN_STATUS)
    val url = cursor.getString(DatabaseHelper.INDEX_COLUMN_URL)
    val file = cursor.getString(DatabaseHelper.INDEX_COLUMN_FILEPATH)
    val error = cursor.getInt(DatabaseHelper.INDEX_COLUMN_ERROR)
    val total = cursor.getLong(DatabaseHelper.INDEX_COLUMN_FILE_SIZE)
    val priority = cursor.getInt(DatabaseHelper.INDEX_COLUMN_PRIORITY)
    val downloaded = cursor.getLong(DatabaseHelper.INDEX_COLUMN_DOWNLOADED_BYTES)
    val headers = cursor.getString(DatabaseHelper.INDEX_COLUMN_HEADERS)

    val downloadInfo = databaseManagerWrapper.getNewDownloadInfoInstance()
    downloadInfo.id = (url.hashCode() * 31) + file.hashCode()
    downloadInfo.url = url
    downloadInfo.file = file
    downloadInfo.status = getStatusFromV1ForV2(status)
    downloadInfo.total = total
    downloadInfo.downloaded = downloaded
    downloadInfo.headers = fromHeaderStringToMap(headers)
    downloadInfo.priority = getPriorityFromV1ForV2(priority)
    downloadInfo.error = getErrorFromV1ForV2(error)
    downloadInfo.enqueueAction = EnqueueAction.REPLACE_EXISTING
    downloadInfo.identifier = downloadInfo.id.toLong()
    downloadInfo.downloadOnEnqueue = DEFAULT_DOWNLOAD_ON_ENQUEUE
    downloadInfo.extras = Extras.emptyExtras
    return DownloadTransferPair(downloadInfo, id)
}

fun getStatusFromV1ForV2(v1StatusCode: Int): Status {
    return when (v1StatusCode) {
        FetchConst.STATUS_QUEUED -> Status.QUEUED
        FetchConst.STATUS_DOWNLOADING -> Status.DOWNLOADING
        FetchConst.STATUS_DONE -> Status.COMPLETED
        FetchConst.STATUS_ERROR -> Status.FAILED
        FetchConst.STATUS_PAUSED -> Status.PAUSED
        FetchConst.STATUS_REMOVED -> Status.REMOVED
        FetchConst.STATUS_NOT_QUEUED -> Status.NONE
        else -> Status.NONE
    }
}

fun getPriorityFromV1ForV2(v1PriorityCode: Int): Priority {
    return when (v1PriorityCode) {
        FetchConst.PRIORITY_HIGH -> Priority.HIGH
        FetchConst.PRIORITY_NORMAL -> Priority.NORMAL
        else -> Priority.LOW
    }
}

fun getErrorFromV1ForV2(v1ErrorCode: Int): Error {
    return when (v1ErrorCode) {
        -1 -> Error.NONE
        FetchConst.ERROR_FILE_NOT_CREATED -> Error.FILE_NOT_CREATED
        FetchConst.ERROR_CONNECTION_TIMEOUT -> Error.CONNECTION_TIMED_OUT
        FetchConst.ERROR_UNKNOWN_HOST -> Error.UNKNOWN_HOST
        FetchConst.ERROR_HTTP_NOT_FOUND -> Error.HTTP_NOT_FOUND
        FetchConst.ERROR_WRITE_PERMISSION_DENIED -> Error.WRITE_PERMISSION_DENIED
        FetchConst.ERROR_NO_STORAGE_SPACE -> Error.NO_STORAGE_SPACE
        FetchConst.ERROR_SERVER_ERROR -> Error.EMPTY_RESPONSE_FROM_SERVER
        FetchConst.ERROR_REQUEST_ALREADY_EXIST -> Error.REQUEST_ALREADY_EXIST
        FetchConst.ERROR_ENQUEUE_ERROR -> Error.FETCH_DATABASE_ERROR
        else -> Error.UNKNOWN
    }
}

fun fromHeaderStringToMap(headerString: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val json = JSONObject(headerString)
    json.keys().forEach {
        map[it] = json.getString(it)
    }
    return map
}