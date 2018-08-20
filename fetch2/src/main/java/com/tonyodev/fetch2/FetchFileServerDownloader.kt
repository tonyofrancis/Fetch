package com.tonyodev.fetch2

import com.tonyodev.fetch2core.*

import com.tonyodev.fetch2core.server.FileRequest.CREATOR.TYPE_FILE
import com.tonyodev.fetch2core.server.FetchFileResourceTransporter
import com.tonyodev.fetch2core.server.FileRequest
import com.tonyodev.fetch2core.server.FileResponse
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader

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

    override fun execute(request: Downloader.ServerRequest, interruptMonitor: InterruptMonitor): Downloader.Response? {
        val headers = request.headers
        val range = getRangeForFetchFileServerRequest(headers["Range"] ?: "bytes=0-")
        val authorization = headers[FileRequest.FIELD_AUTHORIZATION] ?: ""
        val port = getFetchFileServerPort(request.url)
        val address = getFetchFileServerHostAddress(request.url)
        val extras = request.extras.toMutableExtras()
        request.headers.forEach {
            extras.putString(it.key, it.value)
        }
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
                extras = extras,
                page = headers[FileRequest.FIELD_PAGE]?.toIntOrNull()
                        ?: 0,
                size = headers[FileRequest.FIELD_SIZE]?.toIntOrNull()
                        ?: 0,
                persistConnection = false)
        transporter.sendFileRequest(fileRequest)
        while (!interruptMonitor.isInterrupted) {
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
            val transporter = connections[response]
            connections.remove(response)
            transporter?.close()
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

    override fun getRequestFileDownloaderType(request: Downloader.ServerRequest, supportedFileDownloaderTypes: Set<Downloader.FileDownloaderType>): Downloader.FileDownloaderType {
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

    override fun getHeadRequestMethodSupported(request: Downloader.ServerRequest): Boolean {
        return false
    }

    override fun getRequestBufferSize(request: Downloader.ServerRequest): Int {
        return DEFAULT_BUFFER_SIZE
    }

    override fun getRequestContentLength(request: Downloader.ServerRequest): Long {
        return getRequestContentLength(request, this)
    }

    override fun getFetchFileServerCatalog(serverRequest: Downloader.ServerRequest): List<FileResource> {
        val response = execute(serverRequest, object : InterruptMonitor {
            override val isInterrupted: Boolean
                get() = false
        })
        if (response?.byteStream != null) {
            try {
                val type = response.responseHeaders[FileRequest.FIELD_TYPE]?.firstOrNull()?.toInt()
                        ?: -1
                if (type != FileRequest.TYPE_FILE) {
                    disconnect(response)
                    throw Exception(FETCH_FILE_SERVER_INVALID_RESPONSE_TYPE)
                }
                val bufferSize = 1024
                val buffer = CharArray(bufferSize)
                val stringBuilder = StringBuilder()
                val inputReader = InputStreamReader(response.byteStream, Charsets.UTF_8)
                var read = inputReader.read(buffer, 0, bufferSize)
                while (read != -1) {
                    stringBuilder.append(buffer, 0, read)
                    read = inputReader.read(buffer, 0, bufferSize)
                }
                inputReader.close()
                val data = stringBuilder.toString()
                if (data.isNotEmpty()) {
                    val json = JSONObject(data)
                    val catalogArray = JSONArray(json.getString("catalog"))
                    val size = catalogArray.length()
                    val fileResourceList = mutableListOf<FileResource>()
                    for (index in 0 until size) {
                        val fileResource = FileResource()
                        val catalogItem = catalogArray.getJSONObject(index)
                        fileResource.id = catalogItem.getLong("id")
                        fileResource.name = catalogItem.getString("name")
                        fileResource.length = catalogItem.getLong("length")
                        fileResource.extras = try {
                            val map = mutableMapOf<String, String>()
                            val customJson = JSONObject(catalogItem.getString("extras"))
                            customJson.keys().forEach {
                                map[it] = customJson.getString(it)
                            }
                            Extras(map)
                        } catch (e: Exception) {
                            Extras.emptyExtras
                        }
                        fileResource.md5 = catalogItem.getString("md5")
                        fileResourceList.add(fileResource)
                    }
                    disconnect(response)
                    return fileResourceList
                } else {
                    throw Exception(EMPTY_RESPONSE_BODY)
                }
            } catch (e: Exception) {
                disconnect(response)
                throw e
            }
        } else {
            throw Exception(EMPTY_RESPONSE_BODY)
        }
    }

    override fun getRequestSupportedFileDownloaderTypes(request: Downloader.ServerRequest): Set<Downloader.FileDownloaderType> {
        return try {
            getRequestSupportedFileDownloaderTypes(request, this)
        } catch (e: Exception) {
            mutableSetOf(fileDownloaderType)
        }
    }

}