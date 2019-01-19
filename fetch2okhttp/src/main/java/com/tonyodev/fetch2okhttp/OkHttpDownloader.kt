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
    : Downloader<OkHttpClient, Request> {

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
            .cookieJar(getDefaultCookieJar())
            .build()

    override fun onPreClientExecute(client: OkHttpClient, request: Downloader.ServerRequest): Request {
        val okHttpRequestBuilder = Request.Builder()
                .url(request.url)
                .method(request.requestMethod, null)
        request.headers.entries.forEach {
            okHttpRequestBuilder.addHeader(it.key, it.value)
        }
        return okHttpRequestBuilder.build()
    }

    override fun execute(request: Downloader.ServerRequest, interruptMonitor: InterruptMonitor): Downloader.Response? {
        var okHttpRequest = onPreClientExecute(client, request)
        if (okHttpRequest.header("Referer") == null) {
            val referer = getRefererFromUrl(request.url)
            okHttpRequest = okHttpRequest.newBuilder()
                    .addHeader("Referer", referer)
                    .build()
        }
        val okHttpResponse = client.newCall(okHttpRequest).execute()
        val code = okHttpResponse.code()
        val success = okHttpResponse.isSuccessful
        var contentLength = okHttpResponse.body()?.contentLength() ?: -1L
        val byteStream: InputStream? = okHttpResponse.body()?.byteStream()
        val responseHeaders = mutableMapOf<String, List<String>>()
        val errorResponseString: String? = if (!success) {
            copyStreamToString(byteStream, false)
        } else {
            null
        }
        val okResponseHeaders = okHttpResponse.headers()
        for (i in 0 until okResponseHeaders.size()) {
            val key = okResponseHeaders.name(i)
            val values = okResponseHeaders.values(key)
            responseHeaders[key] = values
        }
        val hash = getContentHash(responseHeaders)

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
                hash = hash,
                responseHeaders = responseHeaders,
                acceptsRanges = acceptsRanges,
                errorResponse = errorResponseString))

        val response = Downloader.Response(
                code = code,
                isSuccessful = success,
                contentLength = contentLength,
                byteStream = byteStream,
                request = request,
                hash = hash,
                responseHeaders = responseHeaders,
                acceptsRanges = acceptsRanges,
                errorResponse = errorResponseString)

        connections[response] = okHttpResponse
        return response
    }

    override fun getContentHash(responseHeaders: MutableMap<String, List<String>>): String {
        return responseHeaders["Content-MD5"]?.firstOrNull() ?: ""
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

    override fun getFileSlicingCount(request: Downloader.ServerRequest, contentLength: Long): Int? {
        return null
    }

    override fun getRequestFileDownloaderType(request: Downloader.ServerRequest, supportedFileDownloaderTypes: Set<Downloader.FileDownloaderType>): Downloader.FileDownloaderType {
        return fileDownloaderType
    }

    override fun verifyContentHash(request: Downloader.ServerRequest, hash: String): Boolean {
        if (hash.isEmpty()) {
            return true
        }
        val fileMd5 = getFileMd5String(request.file)
        return fileMd5?.contentEquals(hash) ?: true
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