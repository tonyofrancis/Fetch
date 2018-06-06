package com.tonyodev.fetch2fileserver

import android.net.Uri

class FetchFileServerUrlBuilder {

    private var host = "00:00:00:00"
    private var port = 0
    private var path = ""

    fun setHostAddress(hostAddress: String): FetchFileServerUrlBuilder {
        this.host = hostAddress
        return this
    }

    fun setHostPort(port: Int): FetchFileServerUrlBuilder {
        this.port = port
        return this
    }

    fun setHostInetAddress(hostAddress: String, port: Int): FetchFileServerUrlBuilder {
        this.port = port
        this.host = hostAddress
        return this
    }

    fun setPath(path: String): FetchFileServerUrlBuilder {
        this.path = path
        return this
    }

    fun setPath(path: Long): FetchFileServerUrlBuilder {
        this.path = path.toString()
        return this
    }

    fun create(): String {
        return Uri.Builder()
                .scheme(FETCH_URL_SCHEME)
                .encodedAuthority("$host:$port")
                .appendPath(path)
                .toString()
    }

    companion object {
        const val FETCH_URL_SCHEME = "fetchlocal"
    }

}