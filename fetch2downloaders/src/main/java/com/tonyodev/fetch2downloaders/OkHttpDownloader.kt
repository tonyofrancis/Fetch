package com.tonyodev.fetch2downloaders

import com.tonyodev.fetch2.Downloader
import com.tonyodev.fetch2.util.InterruptMonitor
import com.tonyodev.fetch2.util.getFileMd5String
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * This downloader uses a OkHttpClient to perform http requests.
 * You can also pass in your custom okHttpClient for this downloader to use.
 * @see {@link com.tonyodev.fetch2.Downloader}
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

    override fun execute(request: Downloader.Request, interruptMonitor: InterruptMonitor?): Downloader.Response? {
        val okHttpRequestBuilder = Request.Builder()
                .url(request.url)

        request.headers.entries.forEach {
            okHttpRequestBuilder.addHeader(it.key, it.value)
        }

        val okHttpRequest = okHttpRequestBuilder.build()
        val okHttpResponse = client.newCall(okHttpRequest).execute()
        val code = okHttpResponse.code()
        val success = okHttpResponse.isSuccessful
        val contentLength = okHttpResponse.body()?.contentLength() ?: -1L
        val byteStream: InputStream? = okHttpResponse.body()?.byteStream()
        val md5 = okHttpResponse.header("Content-MD5") ?: ""

        val response = Downloader.Response(
                code = code,
                isSuccessful = success,
                contentLength = contentLength,
                byteStream = byteStream,
                request = request,
                md5 = md5)

        connections[response] = okHttpResponse
        return response
    }

    override fun disconnect(response: Downloader.Response) {
        if (connections.contains(response)) {
            val okHttpResponse = connections[response] as Response
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

    override fun getRequestOutputStream(request: Downloader.Request, filePointerOffset: Long): OutputStream? {
        return null
    }

    override fun getFileSlicingCount(request: Downloader.Request, contentLength: Long): Int? {
        return null
    }

    override fun getDirectoryForFileDownloaderTypeParallel(request: Downloader.Request): String? {
        return null
    }

    override fun seekOutputStreamToPosition(request: Downloader.Request, outputStream: OutputStream, filePointerOffset: Long) {

    }

    override fun getFileDownloaderType(request: Downloader.Request): Downloader.FileDownloaderType {
        return fileDownloaderType
    }

    override fun verifyContentMD5(request: Downloader.Request, md5: String): Boolean {
        if (md5.isEmpty()) {
            return true
        }
        val fileMd5 = getFileMd5String(request.file)
        return fileMd5?.contentEquals(md5) ?: true
    }

}