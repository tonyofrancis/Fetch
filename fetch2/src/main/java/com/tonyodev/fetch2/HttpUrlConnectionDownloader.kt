package com.tonyodev.fetch2

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import kotlin.collections.HashMap

/**
 * The default Downloader used by Fetch for downloading requests.
 * This downloader uses a HttpUrlConnection to perform http requests
 * @see {@link com.tonyodev.fetch2.Downloader}
 * */
open class HttpUrlConnectionDownloader @JvmOverloads constructor(
        /**
         * HttpUrlConnectionPreferences to set preference settings for the
         * HttpUrlConnectionDownloader HttpUrlConnection client.
         * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader.HttpUrlConnectionPreferences
         * */
        httpUrlConnectionPreferences: HttpUrlConnectionPreferences? = null) : Downloader {

    val connectionPrefs = httpUrlConnectionPreferences ?: HttpUrlConnectionPreferences()
    val connections = Collections.synchronizedMap(HashMap<Downloader.Response, HttpURLConnection>())

    override fun execute(request: Downloader.Request): Downloader.Response? {
        val httpUrl = URL(request.url)
        val client = httpUrl.openConnection() as HttpURLConnection
        client.requestMethod = "GET"
        client.readTimeout = connectionPrefs.readTimeout
        client.connectTimeout = connectionPrefs.connectTimeout
        client.useCaches = connectionPrefs.usesCache
        client.defaultUseCaches = connectionPrefs.usesDefaultCache
        client.instanceFollowRedirects = connectionPrefs.followsRedirect
        client.doInput = true

        request.headers.entries.forEach {
            client.addRequestProperty(it.key, it.value)
        }

        client.connect()

        val code = client.responseCode
        var success = false
        var contentLength = -1L
        var byteStream: InputStream? = null

        if (isResponseOk(code)) {
            success = true
            contentLength = client.getHeaderField("Content-Length")?.toLong() ?: -1
            byteStream = client.inputStream
        }

        val response = Downloader.Response(
                code = code,
                isSuccessful = success,
                contentLength = contentLength,
                byteStream = byteStream)

        connections[response] = client
        return response
    }

    fun isResponseOk(responseCode: Int): Boolean {
        return when (responseCode) {
            HttpURLConnection.HTTP_OK,
            HttpURLConnection.HTTP_PARTIAL,
            HttpURLConnection.HTTP_ACCEPTED -> true
            else -> false
        }
    }

    override fun disconnect(response: Downloader.Response) {
        if (connections.contains(response)) {
            val client = connections[response] as HttpURLConnection
            connections.remove(response)
            client.disconnect()
        }
    }

    override fun close() {
        connections.entries.forEach {
            it.value.disconnect()
        }
        connections.clear()
    }

    /**
     * Use this class to set preference settings for the
     * HttpUrlConnectionDownloader HttpUrlConnection client.
     * */
    open class HttpUrlConnectionPreferences {
        var readTimeout = 20_000
        var connectTimeout = 15_000
        var usesCache = false
        var usesDefaultCache = false
        var followsRedirect = true
    }

}