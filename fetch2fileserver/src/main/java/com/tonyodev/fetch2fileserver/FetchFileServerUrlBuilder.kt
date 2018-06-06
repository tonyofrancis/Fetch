package com.tonyodev.fetch2fileserver

import android.net.Uri

/**
 * Builder used to create a Fetch File Server url.
 * */
class FetchFileServerUrlBuilder {

    private var host = "00:00:00:00"
    private var port = 0
    private var path = ""

    /** Set the IP address of the fetch file server
     * @param hostAddress ip address
     * @return builder
     * */
    fun setHostAddress(hostAddress: String): FetchFileServerUrlBuilder {
        this.host = hostAddress
        return this
    }

    /** Set the port of the fetch file server
     * @param port port
     * @return builder
     * */
    fun setHostPort(port: Int): FetchFileServerUrlBuilder {
        this.port = port
        return this
    }

    /** Set the IP address and port of the fetch file server
     * @param hostAddress ip address
     * @param port port
     * @return builder
     * */
    fun setHostInetAddress(hostAddress: String, port: Int): FetchFileServerUrlBuilder {
        this.port = port
        this.host = hostAddress
        return this
    }

    /** Set the content file resource path. This could be the content file id or content file name.
     * @param path resource path
     * @return builder
     * */
    fun setPath(path: String): FetchFileServerUrlBuilder {
        this.path = path
        return this
    }

    /** Set the content file resource id path.
     * @param path resource id path
     * @return builder
     * */
    fun setPath(path: Long): FetchFileServerUrlBuilder {
        this.path = path.toString()
        return this
    }

    /**
     * Create Fetch file server url string.
     * */
    fun create(): String {
        return Uri.Builder()
                .scheme(FETCH_URL_SCHEME)
                .encodedAuthority("$host:$port")
                .appendPath(path)
                .toString()
    }

    companion object {

        /** Fetch File Server Url Scheme*/
        const val FETCH_URL_SCHEME = "fetchlocal"

    }

}