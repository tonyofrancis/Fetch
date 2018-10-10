package com.tonyodev.fetch2okhttp

import com.tonyodev.fetch2core.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * This downloader uses a OkHttpClient to perform http requests.
 * You can also pass in your custom okHttpClient for this downloader to use.
 * @see {@link com.tonyodev.fetch2core.Downloader}
 * */
open class OkHttpDownloader @JvmOverloads constructor(
        /** OkHttpClient */
        okHttpClient: OkHttpClient? = null,
        /** The file downloader type used to download a request.
         * The SEQUENTIAL type downloads bytes in sequence.
         * The PARALLEL type downloads bytes in parallel.
         * */
        private val fileDownloaderType: Downloader.FileDownloaderType = Downloader.FileDownloaderType.SEQUENTIAL)
    : Downloader {

    constructor(fileDownloaderType: Downloader.FileDownloaderType) : this(null, fileDownloaderType)

    protected val connections: MutableMap<Downloader.Response, Response> = Collections.synchronizedMap(HashMap<Downloader.Response, Response>())

    @Volatile
    var client: OkHttpClient = okHttpClient ?: OkHttpClient.Builder()
            .readTimeout(20_000L, TimeUnit.MILLISECONDS)
            .connectTimeout(15_000L, TimeUnit.MILLISECONDS)
            .cache(null)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false)
            .build()

    override fun execute(request: Downloader.ServerRequest, interruptMonitor: InterruptMonitor): Downloader.Response? {
        val okHttpRequestBuilder = Request.Builder()
                .url(request.url)
                .method(request.requestMethod, null)

        request.headers.entries.forEach {
            okHttpRequestBuilder.addHeader(it.key, it.value)
        }

        val okHttpRequest = okHttpRequestBuilder.build()
        val okHttpResponse = client.newCall(okHttpRequest).execute()
        val code = okHttpResponse.code()
        val success = okHttpResponse.isSuccessful
        var contentLength = okHttpResponse.body()?.contentLength() ?: -1L
        val byteStream: InputStream? = okHttpResponse.body()?.byteStream()
        val md5 = okHttpResponse.header("Content-MD5") ?: ""
        val responseHeaders = mutableMapOf<String, List<String>>()
        val okResponseHeaders = okHttpResponse.headers()
        for (i in 0 until okResponseHeaders.size()) {
            val key = okResponseHeaders.name(i)
            val values = okResponseHeaders.values(key)
            responseHeaders[key] = values
        }

        if (contentLength < 1) {
            contentLength = responseHeaders["Content-Length"]?.firstOrNull()?.toLong() ?: -1L
        }

        val acceptsRanges = code == HttpURLConnection.HTTP_PARTIAL ||
                responseHeaders["Accept-Ranges"]?.firstOrNull() == "bytes"

        onServerResponse(request, Downloader.Response(
                code = code,
                isSuccessful = success,
                contentLength = contentLength,
                byteStream = null,
                request = request,
                md5 = md5,
                responseHeaders = responseHeaders,
                acceptsRanges = acceptsRanges))

        val response = Downloader.Response(
                code = code,
                isSuccessful = success,
                contentLength = contentLength,
                byteStream = byteStream,
                request = request,
                md5 = md5,
                responseHeaders = responseHeaders,
                acceptsRanges = acceptsRanges)

        connections[response] = okHttpResponse
        return response
    }

    override fun disconnect(response: Downloader.Response) {
        if (connections.contains(response)) {
            val okHttpResponse = connections[response]
            connections.remove(response)
            closeResponse(okHttpResponse)
        }
    }

    override fun close() {
        connections.entries.forEach {
            closeResponse(it.value)
        }
        connections.clear()
    }

    private fun closeResponse(response: Response?) {
        try {
            response?.close()
        } catch (e: Exception) {

        }
    }

    override fun getRequestOutputResourceWrapper(request: Downloader.ServerRequest): OutputResourceWrapper? {
        return null
    }

    override fun getFileSlicingCount(request: Downloader.ServerRequest, contentLength: Long): Int? {
        return null
    }

    override fun getDirectoryForFileDownloaderTypeParallel(request: Downloader.ServerRequest): String? {
        return null
    }

    override fun getRequestFileDownloaderType(request: Downloader.ServerRequest, supportedFileDownloaderTypes: Set<Downloader.FileDownloaderType>): Downloader.FileDownloaderType {
        return fileDownloaderType
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

    override fun getRequestSupportedFileDownloaderTypes(request: Downloader.ServerRequest): Set<Downloader.FileDownloaderType> {
        return try {
            getRequestSupportedFileDownloaderTypes(request, this)
        } catch (e: Exception) {
            mutableSetOf(fileDownloaderType)
        }
    }

}