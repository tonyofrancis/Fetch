package com.tonyodev.fetch2core

import android.net.Uri

/**
 * Builder used to create a Fetch File Server url.
 * */
class FetchFileServerUriBuilder {

    private var host = "00:00:00:00"
    private var port = 0
    private var identifier = ""

    /** Set the IP address of the fetch file server
     * @param hostAddress ip address
     * @return builder
     * */
    fun setHostAddress(hostAddress: String): FetchFileServerUriBuilder {
        this.host = hostAddress
        return this
    }

    /** Set the port of the fetch file server
     * @param port port
     * @return builder
     * */
    fun setHostPort(port: Int): FetchFileServerUriBuilder {
        this.port = port
        return this
    }

    /** Set the IP address and port of the fetch file server
     * @param hostAddress ip address
     * @param port port
     * @return builder
     * */
    fun setHostInetAddress(hostAddress: String, port: Int): FetchFileServerUriBuilder {
        this.port = port
        this.host = hostAddress
        return this
    }

    /** Set the file resource identifier. This could be the content file id or content file name.
     * @param fileResourceName resource identifier
     * @return builder
     * */
    fun setFileResourceIdentifier(fileResourceName: String): FetchFileServerUriBuilder {
        this.identifier = fileResourceName
        return this
    }

    /** Set the file resource id identifier.
     * @param fileResourceId resource id identifier
     * @return builder
     * */
    fun setFileResourceIdentifier(fileResourceId: Long): FetchFileServerUriBuilder {
        this.identifier = fileResourceId.toString()
        return this
    }

    /**
     * Create Fetch file server URI.
     * */
    fun build(): Uri {
        return Uri.Builder()
                .scheme(FETCH_URI_SCHEME)
                .encodedAuthority("$host:$port")
                .appendPath(identifier)
                .build()
    }

    override fun toString(): String {
        return build().toString()
    }

    companion object {

        /** Fetch File Server Url Scheme*/
        const val FETCH_URI_SCHEME = "fetchlocal"

    }

}