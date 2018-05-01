package com.tonyodev.fetch2

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

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
     * the response back to the Fetch FileDownloader for processing. This method
     * is called on a background thread.
     * @param request The request information for the download.
     * @return Response containing the server response code, connection success, content-length
     * and input stream if a connection was successful.
     * For an example:
     * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader.execute
     * */
    fun execute(request: Request): Response?

    /**
     * This method is called by Fetch to disconnect the connection for the passed in response.
     * Perform any clean against the passed in response. This method is called on a background thread.
     * @param response A response that was returned by the execute method.
     * For an example:
     * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader.disconnect
     * */
    fun disconnect(response: Response)

    /**
     * This method is called by Fetch to request the output stream the download will be saved too.
     * The output stream for the request needs to be the same each time. Note that the type of
     * output stream may affect download speeds.
     * If null is returned, Fetch will provide the output stream. This method is called on a background thread.
     * @param request The request information for the download.
     * @param filePointerOffset The offset position, measured in bytes from the beginning of the file,
     *                          at which to set the file pointer. Writing will begin at the
     *                          filePointerOffset position. Note that your output stream needs
     *                          to use this value to set the file pointer location
     *                          so that data in the file is not overwritten. If not
     *                          handled correctly, Fetch will override the file and being writing
     *                          data at the beginning of the file.
     * @return The output stream the download will be saved to. Fetch will close the output stream automatically
     *         after the disconnect(response) method is called. Can return null. If null,
     *         Fetch will provide the output stream.
     * */
    fun getRequestOutputStream(request: Request, filePointerOffset: Long): OutputStream?

    /**
     * This method is called by Fetch to request the input stream where downloaded data was saved to.
     * The input stream for the request needs to be the same each time. Note that the type of
     * input stream may affect download speeds.
     * If null is returned, Fetch will provide the input stream. This method is called on a background thread.
     * @param request The request information for the download.
     * @param filePointerOffset The offset position, measured in bytes from the beginning of the file,
     *                          at which to set the file pointer. Reading will begin at the
     *                          filePointerOffset position. Note that your input stream needs
     *                          to use this value to set the file pointer location
     *                          so that data in the file will be read correctly.
     * @return The input stream the downloaded data will be read from. Fetch will close the input stream automatically
     *         after the disconnect(response) method is called. Can return null. If null,
     *         Fetch will provide the input stream.
     * */
    fun getRequestInputStream(request: Request, filePointerOffset: Long): InputStream?


    /**
     * This method is called by Fetch if the File Chunk feature is enabled.
     * Return the desired size/ chunk size for each download request. If null is returned
     * Fetch will automatically select an appropriate chunk size based on the content length.
     * @param request The request information for the download.
     * @param fileLengthBytes the total content length in bytes
     * @return the chunk size for the request file. Can be null
     * */
    fun getFileChunkSize(request: Request, fileLengthBytes: Long): Int?

    /**
     * A class that contains the information used by the Downloader to create a connection
     * to the server.
     * */
    open class Request(
            /** The request id.*/
            val id: Int,

            /** The url where the file will be downloaded from.*/
            val url: String,

            /** The headers used by the downloader to send header information to
             * the server about a request.*/
            val headers: Map<String, String>,

            /** The file where the download will be stored.*/
            val file: String,

            /** The tag associated with this request.*/
            val tag: String?)

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
            val byteStream: InputStream?,

            /** The request that initiated this response.*/
            val request: Request)

}