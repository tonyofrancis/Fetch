package com.tonyodev.fetch2downloaders

import com.tonyodev.fetch2.Downloader
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * This downloader uses a OkHttpClient to perform http requests.
 * You can also pass in your custom okHttpClient for this downloader to use.
 * @see {@link com.tonyodev.fetch2.Downloader}
 * */
open class OkHttpDownloader @JvmOverloads constructor(okHttpClient: OkHttpClient? = null)
    : Downloader {

    protected val connections: MutableMap<Downloader.Response, Response>
            = Collections.synchronizedMap(HashMap<Downloader.Response, Response>())

    @Volatile
    var client: OkHttpClient = okHttpClient ?: OkHttpClient.Builder()
            .readTimeout(20_000L, TimeUnit.MILLISECONDS)
            .connectTimeout(15_000L, TimeUnit.MILLISECONDS)
            .cache(null)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false)
            .build()

    override fun execute(request: Downloader.Request): Downloader.Response? {
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

        val response = Downloader.Response(
                code = code,
                isSuccessful = success,
                contentLength = contentLength,
                byteStream = byteStream)

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

}