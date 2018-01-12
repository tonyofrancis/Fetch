package com.tonyodev.fetch2

import java.io.Closeable
import java.io.InputStream

/**
 * This interface can be implemented by a class to create a
 * custom Downloader that can be used by Fetch for downloading requests.
 * A custom downloader IS great when you want to provide your own
 * client for Fetch to use for downloading. Note: You are responsible for freeing up resources created
 * by this class when it is no longer needed. Also, the methods in this interface can be called
 * by multiple threads. You are also responsible for making it thread safe when appropriate.
 * For an example see
 * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader
 * */
interface Downloader : Closeable {

    /**
     * This method is called by Fetch to execute a request against the client.
     * This method is responsible for creating and opening a connection then returning
     * the response back to the Fetch FileDownloader for processing.
     * @param request The request information for the download.
     * @return Response containing the server response code, connection success, content-length
     * and input stream if a connection was successful.
     * For an example:
     * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader.execute
     * */
    fun execute(request: Request): Response?

    /**
     * This method is called by Fetch to disconnect the connection for the passed in response.
     * Perform any clean against the passed in response.
     * @param response A response that was returned by the execute method.
     * For an example:
     * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader.disconnect
     * */
    fun disconnect(response: Response)

    /**
     * A class that contains the information used by the Downloader to create a connection
     * to the server.
     * */
    open class Request(
            /** The url where the file will be downloaded from.*/
            val url: String,

            /** The headers used by the downloader to send header information to
             * the server about a request.*/
            val headers: Map<String, String>)

    /**
     * A class that contains the server response information used by Fetch
     * to being the download process.
     * */
    open class Response(
            /** Server response code.*/
            val code: Int,

            /** Indicates if a connection to the server was accepted and successful.*/
            val isSuccessful: Boolean,

            /** The content length of a download.*/
            val contentLength: Long,

            /** The input stream used to perform the download.*/
            val byteStream: InputStream?)

}