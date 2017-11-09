package com.tonyodev.fetch2

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


internal object DownloadHelper {

    fun createHttpRequest(requestData: RequestData): okhttp3.Request {

        val file = File(requestData.absoluteFilePath)
        val builder = okhttp3.Request.Builder()

        builder.url(requestData.url)

        for (key in requestData.headers.keys) {
            requestData.headers[key]?.let { builder.addHeader(key, it) }
        }

        builder.addHeader("Range", "bytes=" + file.length() + "-")
        return builder.build()
    }

    fun calculateProgress(downloadedBytes: Long, fileSize: Long): Int {

        return if (fileSize < 1 || downloadedBytes < 1) {
            0
        } else if (downloadedBytes >= fileSize) {
            100
        } else {
            (downloadedBytes.toDouble() / fileSize.toDouble() * 100).toInt()
        }
    }

    fun hasTwoSecondsPassed(startTime: Long, stopTime: Long): Boolean {
        return TimeUnit.NANOSECONDS.toSeconds(stopTime - startTime) >= 2
    }

    @Throws(IOException::class)
    fun createFileOrThrow(filePath: String): File {

        val file = File(filePath)

        if (file.exists()) {
            return file
        }

        val parentDirCreated = createDirIfNotExist(file.parentFile.absolutePath)
        val fileCreated = createFileIfNotExist(file.absolutePath)

        if (!parentDirCreated || !fileCreated) {
            throw IOException("File could not be created for the filePath:" + filePath)
        }

        return File(filePath)
    }

    private fun createDirIfNotExist(path: String): Boolean {
        val dir = File(path)
        return dir.exists() || dir.mkdirs()
    }

    @Throws(IOException::class)
    private fun createFileIfNotExist(path: String): Boolean {
        val file = File(path)
        return file.exists() || file.createNewFile()
    }

    fun canRetry(status: Status): Boolean {
        return when (status) {
            Status.COMPLETED -> false
            else -> true
        }
    }

    fun canCancel(status: Status): Boolean {
        return when (status) {
            Status.CANCELLED -> false
            else -> true
        }
    }
}
