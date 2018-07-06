package com.tonyodev.fetch2downloaders

import com.tonyodev.fetch2core.calculateProgress
import com.tonyodev.fetch2core.getFile
import java.io.*

/**
 * Use this task to download a file from a FetchFileServer.
 * */
open class FileDownloadTask @JvmOverloads constructor(
        /** resource Identifier. FileResource.Name or FileResource.Id*/
        private val resourceIdentifier: String,
        /** Absolute file path where the downloaded file will be saved.*/
        private val file: String,
        /* File Server host ip address*/
        private val hostAddress: String,
        /** File Server port */
        private val port: Int,
        /** Authorization token*/
        private val authorizationToken: String = "",
        /** Headers*/
        private val headers: Map<String, String> = mapOf()) : FetchFileResourceDownloadTask<File>() {

    override fun getRequest(): FetchFileResourceDownloadTask.FileResourceRequest {
        val fileResourceRequest = FetchFileResourceDownloadTask.FileResourceRequest()
        fileResourceRequest.hostAddress = hostAddress
        fileResourceRequest.port = port
        fileResourceRequest.resourceIdentifier = resourceIdentifier
        fileResourceRequest.addHeader("Authorization", authorizationToken)
        headers.forEach {
            fileResourceRequest.addHeader(it.key, it.value)
        }
        return fileResourceRequest
    }

    override fun doWork(inputStream: InputStream, contentLength: Long, md5CheckSum: String): File {
        val file = getFile(file)
        val bufferedInputStream = BufferedInputStream(inputStream)
        val outputStream = BufferedOutputStream(FileOutputStream(file))
        val buffer = ByteArray(1024)
        var readBytes = 0
        var read: Int = bufferedInputStream.read(buffer, 0, 1024)
        while (read != -1 && !isCancelled) {
            readBytes += read
            outputStream.write(buffer, 0, read)
            setProgress(calculateProgress(readBytes.toLong(), contentLength))
            read = bufferedInputStream.read(buffer, 0, 1024)
        }
        setProgress(100)
        bufferedInputStream.close()
        outputStream.flush()
        outputStream.close()
        return file
    }


}