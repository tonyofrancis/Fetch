@file:JvmName("FetchTypeConverterExtensions")

package com.tonyodev.fetch2.util

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.database.DownloadInfo

fun Request.toDownloadInfo(): DownloadInfo {
    val downloadInfo = DownloadInfo()
    downloadInfo.id = id
    downloadInfo.url = url
    downloadInfo.file = file
    downloadInfo.priority = priority
    downloadInfo.headers = headers
    downloadInfo.group = groupId
    downloadInfo.networkType = networkType
    downloadInfo.status = defaultStatus
    downloadInfo.error = defaultNoError
    downloadInfo.downloaded = getFileLength(file)
    return downloadInfo
}

fun Download.toDownloadInfo(): DownloadInfo {
    val downloadInfo = DownloadInfo()
    downloadInfo.id = id
    downloadInfo.namespace = namespace
    downloadInfo.url = url
    downloadInfo.file = file
    downloadInfo.group = group
    downloadInfo.priority = priority
    downloadInfo.headers = headers
    downloadInfo.downloaded = downloaded
    downloadInfo.total = total
    downloadInfo.status = status
    downloadInfo.networkType = networkType
    downloadInfo.error = error
    downloadInfo.created = created
    return downloadInfo
}