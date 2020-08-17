@file:JvmName("FetchCoreUtils")

package com.tonyodev.fetch2core

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import java.io.*
import java.math.BigInteger
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil

const val GET_REQUEST_METHOD = "GET"

const val HEAD_REQUEST_METHOD = "HEAD"

internal const val HEADER_ACCEPT_RANGE = "Accept-Ranges"

internal const val HEADER_ACCEPT_RANGE_LEGACY = "accept-ranges"

internal const val HEADER_ACCEPT_RANGE_COMPAT = "AcceptRanges"

internal const val HEADER_CONTENT_LENGTH = "content-length"

internal const val HEADER_CONTENT_LENGTH_LEGACY = "Content-Length"

internal const val HEADER_CONTENT_LENGTH_COMPAT = "ContentLength"

internal const val HEADER_TRANSFER_ENCODING = "Transfer-Encoding"

internal const val HEADER_TRANSFER_LEGACY = "transfer-encoding"

internal const val HEADER_TRANSFER_ENCODING_COMPAT = "TransferEncoding"

internal const val HEADER_CONTENT_RANGE = "Content-Range"

internal const val HEADER_CONTENT_RANGE_LEGACY = "content-range"

internal const val HEADER_CONTENT_RANGE_COMPAT = "ContentRange"

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
            val newFileName = "$fileName ($counter)"
            file = File("$parentPath$newFileName.$extension")
        }
    }
    createFile(file)
    return file
}

fun createFile(file: File) {
    if (!file.exists()) {
        if (file.parentFile != null && !file.parentFile.exists()) {
            if (file.parentFile.mkdirs()) {
                if (!file.createNewFile()) throw FileNotFoundException("$file $FILE_NOT_FOUND")
            } else {
                throw FileNotFoundException("$file $FILE_NOT_FOUND")
            }
        } else {
            if (!file.createNewFile()) throw FileNotFoundException("$file $FILE_NOT_FOUND")
        }
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
    return Uri.parse(url).lastPathSegment ?: "-1"
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

@Suppress("ControlFlowWithEmptyBody")
fun getMd5String(bytes: ByteArray, start: Int = 0, length: Int = bytes.size): String {
    return try {
        val buffer = ByteArray(kotlin.io.DEFAULT_BUFFER_SIZE)
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

@Suppress("ControlFlowWithEmptyBody")
fun getFileMd5String(file: String): String? {
    val contentFile = File(file)
    return try {
        val buffer = ByteArray(kotlin.io.DEFAULT_BUFFER_SIZE)
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

fun isParallelDownloadingSupported(code: Int, headers: Map<String, List<String>>): Boolean {
    return acceptRanges(code, headers)
}

fun getRequestSupportedFileDownloaderTypes(
        request: Downloader.ServerRequest,
        downloader: Downloader<*, *>
): Set<Downloader.FileDownloaderType> {
    val fileDownloaderTypeSet = mutableSetOf(Downloader.FileDownloaderType.SEQUENTIAL)
    return try {
        val response = downloader.execute(request, getSimpleInterruptMonitor())
        if (response != null) {
            if (isParallelDownloadingSupported(response.code, response.responseHeaders)) {
                fileDownloaderTypeSet.add(Downloader.FileDownloaderType.PARALLEL)
            }
            downloader.disconnect(response)
        }
        fileDownloaderTypeSet
    } catch (e: Exception) {
        fileDownloaderTypeSet
    }
}

@SuppressLint("DefaultLocale")
fun acceptRanges(
        code: Int,
        headers: Map<String, List<String>>
): Boolean {
    val acceptRangeValue = getHeaderValue(
            headers,
            HEADER_ACCEPT_RANGE,
            HEADER_ACCEPT_RANGE_LEGACY,
            HEADER_ACCEPT_RANGE_COMPAT
    )
    val transferValue = getHeaderValue(
            headers,
            HEADER_TRANSFER_ENCODING,
            HEADER_TRANSFER_LEGACY,
            HEADER_TRANSFER_ENCODING_COMPAT
    )
    val contentLength = getContentLengthFromHeader(headers, -1L)
    val acceptsRanges = code == HttpURLConnection.HTTP_PARTIAL || acceptRangeValue == "bytes"
    return (contentLength > -1L && acceptsRanges) || (contentLength > -1L && transferValue?.toLowerCase() != "chunked")
}

fun getContentLengthFromHeader(headers: Map<String, List<String>>, defaultValue: Long): Long {
    val contentRange = getHeaderValue(
            headers,
            HEADER_CONTENT_RANGE,
            HEADER_CONTENT_RANGE_LEGACY,
            HEADER_CONTENT_RANGE_COMPAT
    )
    val lastIndexOf = contentRange?.lastIndexOf("/")
    var contentLength = -1L
    if (lastIndexOf != null && lastIndexOf != -1 && lastIndexOf < contentRange.length) {
        contentLength = contentRange.substring(lastIndexOf + 1).toLongOrNull() ?: -1L
    }
    if (contentLength == -1L) {
        contentLength = getHeaderValue(
                headers,
                HEADER_CONTENT_LENGTH,
                HEADER_CONTENT_LENGTH_LEGACY,
                HEADER_CONTENT_LENGTH_COMPAT
        )?.toLongOrNull() ?: defaultValue
    }
    return contentLength
}

fun getHeaderValue(
        headers: Map<String, List<String>>,
        vararg keys: String
): String? {
    for (key in keys) {
        val value = headers[key]?.firstOrNull()
        if (!value.isNullOrBlank()) {
            return value
        }
    }
    return null
}

fun getRequestContentLength(request: Downloader.ServerRequest, downloader: Downloader<*, *>): Long {
    return try {
        val response = downloader.execute(request, getSimpleInterruptMonitor())
        val headers = response?.responseHeaders ?: emptyMap()
        val contentLength = getContentLengthFromHeader(headers, -1L)
        if (response != null) {
            downloader.disconnect(response)
        }
        contentLength
    } catch (e: Exception) {
        -1L
    }
}

fun isUriPath(path: String): Boolean = path.takeIf { it.isNotEmpty() }
        ?.let { it.startsWith("content://") || it.startsWith("file://") }
        ?: false

fun getFileUri(path: String): Uri {
    return when {
        isUriPath(path) -> Uri.parse(path)
        else -> Uri.fromFile(File(path))
    }
}

fun deleteFile(file: File): Boolean {
    return if (file.exists() && file.canWrite()) file.delete() else false
}

fun renameFile(oldFile: File, newFile: File): Boolean {
    return oldFile.renameTo(newFile)
}

fun copyDownloadResponseNoStream(response: Downloader.Response): Downloader.Response {
    return Downloader.Response(response.code, response.isSuccessful, response.contentLength, null,
            response.request, response.hash, response.responseHeaders, response.acceptsRanges, response.errorResponse)
}

fun getDefaultCookieManager(): CookieManager {
    val cookieManager = CookieManager()
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    return cookieManager
}

fun hasAllowedTimeExpired(timeStartedMillis: Long, timeStopMillis: Long, allowedTimeMillis: Long): Boolean {
    return timeStopMillis - timeStartedMillis >= allowedTimeMillis
}

fun getRefererFromUrl(url: String): String {
    return try {
        val uri = Uri.parse(url)
        "${uri.scheme}://${uri.authority}"
    } catch (e: Exception) {
        "https://google.com"
    }
}

fun copyStreamToString(inputStream: InputStream?, closeStream: Boolean = true): String? {
    return if (inputStream == null) {
        return null
    } else {
        var bufferedReader: BufferedReader? = null
        try {
            bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String? = bufferedReader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                        .append('\n')
                line = bufferedReader.readLine()
            }
            stringBuilder.toString()
        } catch (e: Exception) {
            null
        } finally {
            if (closeStream) {
                try {
                    bufferedReader?.close()
                } catch (e: Exception) {
                }
            }
        }
    }
}

fun getSimpleInterruptMonitor() = object : InterruptMonitor {
    override val isInterrupted: Boolean
        get() = false
}