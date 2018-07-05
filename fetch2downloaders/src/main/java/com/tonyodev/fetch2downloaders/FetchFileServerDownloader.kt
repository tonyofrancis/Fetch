package com.tonyodev.fetch2downloaders

import com.tonyodev.fetch2core.*

import com.tonyodev.fetch2core.transporter.FileRequest.Companion.TYPE_FILE
import com.tonyodev.fetch2core.transporter.FetchFileResourceTransporter
import com.tonyodev.fetch2core.transporter.FileRequest
import com.tonyodev.fetch2core.transporter.FileResponse
import org.json.JSONObject

import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.util.*

/**
 * This downloader is used by Fetch to download files from a Fetch File Server using the
 * Fetch file server url scheme.
 * @see {@link com.tonyodev.fetch2core.Downloader}
 * */
open class FetchFileServerDownloader @JvmOverloads constructor(

        /** The file downloader type used to download a request.
         * The SEQUENTIAL type downloads bytes in sequence.
         * The PARALLEL type downloads bytes in parallel.
         * */
        private val fileDownloaderType: Downloader.FileDownloaderType = Downloader.FileDownloaderType.SEQUENTIAL,

        /** The timeout value in milliseconds when trying to connect to the server. Default is 20_000 milliseconds. */
        private val timeout: Long = 20_000) : FileServerDownloader {

    protected val connections: MutableMap<Downloader.Response, FetchFileResourceTransporter> = Collections.synchronizedMap(HashMap<Downloader.Response, FetchFileResourceTransporter>())

    override fun execute(request: Downloader.ServerRequest, interruptMonitor: InterruptMonitor?): Downloader.Response? {
        val headers = request.headers
        val range = getRangeForFetchFileServerRequest(headers["Range"] ?: "bytes=0-")
        val authorization = headers[FileRequest.FIELD_AUTHORIZATION] ?: ""
        val port = getFetchFileServerPort(request.url)
        val address = getFetchFileServerHostAddress(request.url)
        val inetSocketAddress = InetSocketAddress(address, port)
        val transporter = FetchFileResourceTransporter()
        var timeoutStop: Long
        val timeoutStart = System.nanoTime()
        transporter.connect(inetSocketAddress)
        val fileRequest = FileRequest(
                type = TYPE_FILE,
                fileResourceId = getFileResourceIdFromUrl(request.url),
                rangeStart = range.first,
                rangeEnd = range.second,
                authorization = authorization,
                client = headers[FileRequest.FIELD_CLIENT]
                        ?: UUID.randomUUID().toString(),
                customData = headers[FileRequest.FIELD_CUSTOM_DATA]
                        ?: "",
                page = headers[FileRequest.FIELD_PAGE]?.toIntOrNull()
                        ?: 0,
                size = headers[FileRequest.FIELD_SIZE]?.toIntOrNull()
                        ?: 0,
                persistConnection = false)
        transporter.sendFileRequest(fileRequest)
        while (interruptMonitor?.isInterrupted == false) {
            val serverResponse = transporter.receiveFileResponse()
            if (serverResponse != null) {
                val code = serverResponse.status
                val isSuccessful = serverResponse.connection == FileResponse.OPEN_CONNECTION &&
                        serverResponse.type == FileRequest.TYPE_FILE && serverResponse.status == HttpURLConnection.HTTP_PARTIAL
                val contentLength = serverResponse.contentLength
                val inputStream = transporter.getInputStream()

                val responseHeaders = mutableMapOf<String, List<String>>()
                try {
                    val json = JSONObject(serverResponse.toJsonString)
                    json.keys().forEach {
                        responseHeaders[it] = listOf(json.get(it).toString())
                    }
                } catch (e: Exception) {

                }

                val acceptsRanges = code == HttpURLConnection.HTTP_PARTIAL ||
                        responseHeaders["Accept-Ranges"]?.firstOrNull() == "bytes"

                onServerResponse(request, Downloader.Response(
                        code = code,
                        isSuccessful = isSuccessful,
                        contentLength = contentLength,
                        byteStream = null,
                        request = request,
                        md5 = serverResponse.md5,
                        responseHeaders = responseHeaders,
                        acceptsRanges = acceptsRanges))

                val response = Downloader.Response(
                        code = code,
                        isSuccessful = isSuccessful,
                        contentLength = contentLength,
                        byteStream = inputStream,
                        request = request,
                        md5 = serverResponse.md5,
                        responseHeaders = responseHeaders,
                        acceptsRanges = acceptsRanges)

                connections[response] = transporter
                return response
            }
            timeoutStop = System.nanoTime()
            if (hasIntervalTimeElapsed(timeoutStart, timeoutStop, timeout)) {
                return null
            }
        }
        return null
    }

    override fun disconnect(response: Downloader.Response) {
        if (connections.contains(response)) {
            val transporter = connections[response] as FetchFileResourceTransporter
            connections.remove(response)
            transporter.close()
        }
    }

    override fun close() {
        try {
            connections.entries.forEach {
                it.value.close()
            }
            connections.clear()
        } catch (e: Exception) {

        }
    }

    override fun getRequestOutputResourceWrapper(request: Downloader.ServerRequest): OutputResourceWrapper? {
        return null
    }

    override fun getFileSlicingCount(request: Downloader.ServerRequest, contentLength: Long): Int? {
        return null
    }

    override fun getFileDownloaderType(request: Downloader.ServerRequest): Downloader.FileDownloaderType {
        return fileDownloaderType
    }

    override fun getDirectoryForFileDownloaderTypeParallel(request: Downloader.ServerRequest): String? {
        return null
    }

    override fun verifyContentMD5(request: Downloader.ServerRequest, md5: String): Boolean {
        if (md5.isEmpty()) {
            return true
        }
        val fileMd5 = getFileMd5String(request.file)
        return fileMd5?.contentEquals(md5) ?: true
    }

    override fun onServerResponse(request: Downloader.ServerRequest, response: Downloader.Response) {

    }

}