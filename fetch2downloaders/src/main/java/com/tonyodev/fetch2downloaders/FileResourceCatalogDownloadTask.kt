package com.tonyodev.fetch2downloaders

import com.tonyodev.fetch2core.FileResource
import com.tonyodev.fetch2core.calculateProgress
import com.tonyodev.fetch2core.server.FileRequest
import org.json.JSONObject
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Use this task to get all the FileResource items hosted by a FetchFileServer instance.
 * */
open class FileResourceCatalogDownloadTask @JvmOverloads constructor(
        /* File Server host ip address*/
        private val hostAddress: String,
        /** File Server port */
        private val port: Int,
        /** Authorization token*/
        private val authorizationToken: String = "",
        /** Headers*/
        private val headers: Map<String, String> = mapOf())
    : FetchFileResourceDownloadTask<List<FileResource>>() {

    override fun getRequest(): FileResourceRequest {
        val fileResourceRequest = FetchFileResourceDownloadTask.FileResourceRequest()
        fileResourceRequest.hostAddress = hostAddress
        fileResourceRequest.port = port
        fileResourceRequest.resourceIdentifier = FileRequest.CATALOG_ID.toString()
        fileResourceRequest.addHeader("Authorization", authorizationToken)
        headers.forEach {
            fileResourceRequest.addHeader(it.key, it.value)
        }
        return fileResourceRequest
    }

    override fun doWork(inputStream: InputStream, contentLength: Long, md5CheckSum: String): List<FileResource> {
        val bufferSize = 1024
        val buffer = CharArray(bufferSize)
        val stringBuilder = StringBuilder()
        val inputReader = InputStreamReader(inputStream, Charsets.UTF_8)
        var readBytes = 0L
        var read = inputReader.read(buffer, 0, bufferSize)
        while (read != -1 && !isCancelled) {
            readBytes += read
            stringBuilder.append(buffer, 0, read)
            setProgress(calculateProgress(readBytes, contentLength))
            read = inputReader.read(buffer, 0, bufferSize)
        }
        inputStream.close()
        return if (!isCancelled) {
            val json = JSONObject(stringBuilder.toString())
            val catalogArray = json.getJSONArray("catalog")
            val size = catalogArray.length()
            val fileResourceList = mutableListOf<FileResource>()
            for (index in 0 until size) {
                if (!isCancelled) {
                    val fileResource = FileResource()
                    val catalogItem = catalogArray.getJSONObject(index)
                    fileResource.id = catalogItem.getLong("id")
                    fileResource.name = catalogItem.getString("name")
                    fileResource.length = catalogItem.getLong("length")
                    fileResource.customData = catalogItem.getString("customData")
                    fileResource.md5 = catalogItem.getString("md5")
                    fileResourceList.add(fileResource)
                } else {
                    return emptyList()
                }
            }
            fileResourceList
        } else {
            listOf()
        }
    }

}