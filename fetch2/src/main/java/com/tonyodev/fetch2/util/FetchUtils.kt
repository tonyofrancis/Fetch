@file:JvmName("FetchUtils")

package com.tonyodev.fetch2.util

import android.content.Context
import android.net.Uri
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Downloader
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.Status
import java.io.*
import java.math.BigInteger
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil


fun canPauseDownload(download: Download): Boolean {
    return when (download.status) {
        Status.DOWNLOADING,
        Status.QUEUED -> true
        else -> false
    }
}

fun canResumeDownload(download: Download): Boolean {
    return when (download.status) {
        Status.PAUSED -> true
        else -> false
    }
}

fun canRetryDownload(download: Download): Boolean {
    return when (download.status) {
        Status.FAILED,
        Status.CANCELLED -> true
        else -> false
    }
}

fun canCancelDownload(download: Download): Boolean {
    return when (download.status) {
        Status.COMPLETED,
        Status.NONE,
        Status.FAILED -> false
        else -> true
    }
}

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

fun getRequestForDownload(download: Download,
                          rangeStart: Long = -1,
                          rangeEnd: Long = -1): Downloader.ServerRequest {
    val start = if (rangeStart == -1L) 0 else rangeStart
    val end = if (rangeEnd == -1L) "" else rangeEnd.toString()
    val headers = download.headers.toMutableMap()
    headers["Range"] = "bytes=$start-$end"
    return Downloader.ServerRequest(
            id = download.id,
            url = download.url,
            headers = headers,
            file = download.file,
            tag = download.tag)
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

fun writeTextToFile(filePath: String, text: String) {
    val file = getFile(filePath)
    if (file.exists()) {
        val bufferedWriter = BufferedWriter(FileWriter(file))
        try {
            bufferedWriter.write(text)
        } catch (e: Exception) {
        } finally {
            try {
                bufferedWriter.close()
            } catch (e: Exception) {
            }
        }
    }
}

fun getSingleLineTextFromFile(filePath: String): String? {
    val file = getFile(filePath)
    if (file.exists()) {
        val bufferedReader = BufferedReader(FileReader(file))
        try {
            return bufferedReader.readLine()
        } catch (e: Exception) {
        } finally {
            try {
                bufferedReader.close()
            } catch (e: Exception) {
            }
        }
    }
    return null
}

fun deleteRequestTempFiles(fileTempDir: String,
                           downloader: Downloader,
                           download: Download) {
    try {
        val request = getRequestForDownload(download)
        val tempDirPath = downloader.getDirectoryForFileDownloaderTypeParallel(request)
                ?: fileTempDir
        val tempDir = getFile(tempDirPath)
        if (tempDir.exists()) {
            val tempFiles = tempDir.listFiles()
            for (tempFile in tempFiles) {
                val match = tempFile.name.startsWith("${download.id}.")
                if (match && tempFile.exists()) {
                    try {
                        tempFile.delete()
                    } catch (e: Exception) {
                    }
                }
            }
        }
    } catch (e: Exception) {

    }
}

fun createConfigWithNewNamespace(fetchConfiguration: FetchConfiguration,
                                 namespace: String): FetchConfiguration {
    return FetchConfiguration.Builder(fetchConfiguration.appContext)
            .setNamespace(namespace)
            .enableAutoStart(fetchConfiguration.autoStart)
            .enableLogging(fetchConfiguration.loggingEnabled)
            .enableRetryOnNetworkGain(fetchConfiguration.retryOnNetworkGain)
            .setDownloadBufferSize(fetchConfiguration.downloadBufferSizeBytes)
            .setHttpDownloader(fetchConfiguration.httpDownloader)
            .setDownloadConcurrentLimit(fetchConfiguration.concurrentLimit)
            .setProgressReportingInterval(fetchConfiguration.progressReportingIntervalMillis)
            .setGlobalNetworkType(fetchConfiguration.globalNetworkType)
            .setLogger(fetchConfiguration.logger)
            .build()
}

//eg: fetchlocal://192.168.0.1:80/45
//eg: fetchlocal://192.168.0.1:80/file.txt
fun isFetchFileServerUrl(url: String): Boolean {
    return url.startsWith("fetchlocal://")
            && getFetchFileServerHostAddress(url).isNotEmpty()
            && getFetchFileServerPort(url) > -1
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
