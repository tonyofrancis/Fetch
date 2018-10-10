@file:JvmName("FetchCoreUtils")

package com.tonyodev.fetch2core

import android.content.Context
import android.net.Uri
import java.io.*
import java.math.BigInteger
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil

const val GET_REQUEST_METHOD = "GET"

const val HEAD_REQUEST_METHOD = "HEAD"

fun calculateProgress(downloaded: Long, total: Long): Int {
    return when {
        total < 1 -> -1
        downloaded < 1 -> 0
        downloaded >= total -> 100
        else -> ((downloaded.toDouble() / total.toDouble()) * 100).toInt()
    }
}

fun calculateEstimatedTimeRemainingInMilliseconds(downloadedBytes: Long,
                                                  totalBytes: Long,
                                                  downloadedBytesPerSecond: Long): Long {
    return when {
        totalBytes < 1 -> -1
        downloadedBytes < 1 -> -1
        downloadedBytesPerSecond < 1 -> -1
        else -> {
            val seconds = (totalBytes - downloadedBytes).toDouble() / downloadedBytesPerSecond.toDouble()
            return abs(ceil(seconds)).toLong() * 1000
        }
    }
}

fun hasIntervalTimeElapsed(nanoStartTime: Long, nanoStopTime: Long,
                           progressIntervalMilliseconds: Long): Boolean {
    return TimeUnit.NANOSECONDS
            .toMillis(nanoStopTime - nanoStartTime) >= progressIntervalMilliseconds
}

fun getUniqueId(url: String, file: String): Int {
    return (url.hashCode() * 31) + file.hashCode()
}

fun getIncrementedFileIfOriginalExists(originalPath: String): File {
    var file = File(originalPath)
    var counter = 0
    if (file.exists()) {
        val parentPath = "${file.parent}/"
        val extension = file.extension
        val fileName: String = file.nameWithoutExtension
        while (file.exists()) {
            ++counter
            val newFileName = "$fileName ($counter) "
            file = File("$parentPath$newFileName.$extension")
        }
    }
    return file
}

fun createFileIfPossible(file: File) {
    try {
        if (!file.exists()) {
            if (file.parentFile != null && !file.parentFile.exists()) {
                if (file.parentFile.mkdirs()) {
                    file.createNewFile()
                }
            } else {
                file.createNewFile()
            }
        }
    } catch (e: IOException) {
    }
}

fun getFileTempDir(context: Context): String {
    return "${context.filesDir.absoluteFile}/_fetchData/temp"
}

fun getFile(filePath: String): File {
    val file = File(filePath)
    if (!file.exists()) {
        if (file.parentFile != null && !file.parentFile.exists()) {
            if (file.parentFile.mkdirs()) {
                file.createNewFile()
            }
        } else {
            file.createNewFile()
        }
    }
    return file
}

fun writeLongToFile(filePath: String, data: Long) {
    val file = getFile(filePath)
    if (file.exists()) {
        val randomAccessFile = RandomAccessFile(file, "rw")
        try {
            randomAccessFile.seek(0)
            randomAccessFile.setLength(0)
            randomAccessFile.writeLong(data)
        } catch (e: Exception) {
        } finally {
            try {
                randomAccessFile.close()
            } catch (e: Exception) {
            }
        }
    }
}

fun getLongDataFromFile(filePath: String): Long? {
    val file = getFile(filePath)
    var data: Long? = null
    if (file.exists()) {
        val randomAccessFile = RandomAccessFile(file, "r")
        try {
            data = randomAccessFile.readLong()
        } catch (e: Exception) {
        } finally {
            try {
                randomAccessFile.close()
            } catch (e: Exception) {
            }
        }
    }
    return data
}

//eg: fetchlocal://192.168.0.1:80/45
//eg: fetchlocal://192.168.0.1:80/file.txt
fun isFetchFileServerUrl(url: String): Boolean {
    return try {
        url.startsWith("fetchlocal://")
                && getFetchFileServerHostAddress(url).isNotEmpty()
                && getFetchFileServerPort(url) > -1
    } catch (e: Exception) {
        false
    }
}

fun getFetchFileServerPort(url: String): Int {
    val colonIndex = url.lastIndexOf(":")
    val modifiedUrl = url.substring(colonIndex + 1, url.length)
    val firstSeparatorIndex = modifiedUrl.indexOf("/")
    return if (firstSeparatorIndex == -1) {
        modifiedUrl.toInt()
    } else {
        modifiedUrl.substring(0, firstSeparatorIndex).toInt()
    }
}

fun getFetchFileServerHostAddress(url: String): String {
    val firstIndexOfDoubleSep = url.indexOf("//")
    val colonIndex = url.lastIndexOf(":")
    return url.substring(firstIndexOfDoubleSep + 2, colonIndex)
}

fun getFileResourceIdFromUrl(url: String): String {
    return Uri.parse(url).lastPathSegment
}

//eg: bytes=10-
fun getRangeForFetchFileServerRequest(range: String): Pair<Long, Long> {
    val firstIndex = range.lastIndexOf("=")
    val lastIndex = range.lastIndexOf("-")
    val start = range.substring(firstIndex + 1, lastIndex).toLong()
    val end = try {
        range.substring(lastIndex + 1, range.length).toLong()
    } catch (e: Exception) {
        -1L
    }
    return Pair(start, end)
}

fun getMd5String(bytes: ByteArray, start: Int = 0, length: Int = bytes.size): String {
    return try {
        val buffer = ByteArray(8192)
        val md = MessageDigest.getInstance("MD5")
        val inputStream = DigestInputStream(ByteArrayInputStream(bytes, start, length), md)
        inputStream.use { dis ->
            while (dis.read(buffer) != -1);
        }
        var md5: String = BigInteger(1, md.digest()).toString(16)
        while (md5.length < 32) {
            md5 = "0$md5"
        }
        md5
    } catch (e: Exception) {
        ""
    }
}

fun getFileMd5String(file: String): String? {
    val contentFile = File(file)
    return try {
        val buffer = ByteArray(8192)
        val md = MessageDigest.getInstance("MD5")
        val inputStream = DigestInputStream(FileInputStream(contentFile), md)
        inputStream.use { dis ->
            while (dis.read(buffer) != -1);
        }
        var md5: String = BigInteger(1, md.digest()).toString(16)
        while (md5.length < 32) {
            md5 = "0$md5"
        }
        md5
    } catch (e: Exception) {
        null
    }
}

fun isParallelDownloadingSupported(responseHeaders: Map<String, List<String>>): Boolean {
    val transferEncoding = responseHeaders["Transfer-Encoding"]?.firstOrNull()
            ?: responseHeaders["TransferEncoding"]?.firstOrNull()
            ?: ""
    val contentLength = (responseHeaders["Content-Length"]?.firstOrNull()?.toLongOrNull()
            ?: responseHeaders["ContentLength"]?.firstOrNull()?.toLongOrNull())
            ?: -1L
    return transferEncoding != "chunked" && contentLength > -1L
}

fun getRequestSupportedFileDownloaderTypes(request: Downloader.ServerRequest, downloader: Downloader): Set<Downloader.FileDownloaderType> {
    val fileDownloaderTypeSet = mutableSetOf(Downloader.FileDownloaderType.SEQUENTIAL)
    return try {
        val response = downloader.execute(request, object : InterruptMonitor {
            override val isInterrupted: Boolean
                get() = false
        })
        if (response != null) {
            if (isParallelDownloadingSupported(response.responseHeaders)) {
                fileDownloaderTypeSet.add(Downloader.FileDownloaderType.PARALLEL)
            }
            downloader.disconnect(response)
        }
        fileDownloaderTypeSet
    } catch (e: Exception) {
        fileDownloaderTypeSet
    }
}

fun getRequestContentLength(request: Downloader.ServerRequest, downloader: Downloader): Long {
    return try {
        val response = downloader.execute(request, object : InterruptMonitor {
            override val isInterrupted: Boolean
                get() = false
        })
        val contentLength = response?.contentLength ?: -1L
        if (response != null) {
            downloader.disconnect(response)
        }
        contentLength
    } catch (e: Exception) {
        -1L
    }
}