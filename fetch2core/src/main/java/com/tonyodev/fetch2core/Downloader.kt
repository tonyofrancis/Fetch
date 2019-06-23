package com.tonyodev.fetch2core

import android.net.Uri
import java.io.Closeable
import java.io.InputStream

/**
 * This interface can be implemented by a class to create a
 * custom Downloader that can be used by Fetch for downloading requests.
 * A custom downloader is great when you want to provide your own
 * client for Fetch to use for downloading. Note: You are responsible for freeing up resources created
 * by this class when it is no longer needed. Also, the methods in this interface can be called
 * by multiple threads. You are also responsible for making it thread safe when appropriate.
 * For an example see
 * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader
 * */
interface Downloader<T, R> : Closeable {

    /**
     * This method is called by Fetch before executing a request against the client.
     * Override this method to setup and configure the client for the request.
     * @param client the client
     * @param request the request
     * @return returns any object that makes sense for the specified downloader. can be null.
     * */
    fun onPreClientExecute(client: T, request: Downloader.ServerRequest): R?

    /**
     * This method is called by Fetch to execute a request against the client.
     * This method is responsible for creating and opening a connection then returning
     * the response back to the Fetch FileDownloader for processing. This method
     * is called on a background thread.
     * @param request The request information for the download.
     * @param interruptMonitor Notifies the downloader that there may be an interruption for the request.
     * If an interruption occurs, the execute method should return quickly.
     * @return Response containing the server response code, headers, connection success, content-length,
     * and input stream if a connection was successful.
     * For an example:
     * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader.execute
     * */
    fun execute(request: ServerRequest, interruptMonitor: InterruptMonitor): Response?

    /**
     * This method is called by Fetch to disconnect the connection for the passed in response.
     * Perform any clean against the passed in response. This method is called on a background thread.
     * @param response Response containing the server response code, headers, connection success, content-length,
     * and input stream if a connection was successful.
     * @see com.tonyodev.fetch2.HttpUrlConnectionDownloader.disconnect
     * */
    fun disconnect(response: Response)

    /**
     * This method is called by Fetch if the FileDownloaderType.Parallel type was set
     * for the download request. Returns the desired slices that the file will be divided in for parallel downloading.
     * If null is returned, Fetch will automatically select an appropriate slicing size based on the content length.
     * This method is called on a background thread.
     * @param request the request information for the download.
     * @param contentLength the total content length in bytes.
     * @return the slicing size for the request file. Can be null.
     * */
    fun getFileSlicingCount(request: ServerRequest, contentLength: Long): Int?

    /** This method is called by Fetch to select the FileDownloaderType for each
     * download request. The Default is FileDownloaderType.SEQUENTIAL.
     * This method is called on a background thread.
     * @param request the request information for the download.
     * @param supportedFileDownloaderTypes a set of file downloader types supported by the request.
     * @return the FileDownloaderType.
     * */
    fun getRequestFileDownloaderType(request: ServerRequest, supportedFileDownloaderTypes: Set<Downloader.FileDownloaderType>): FileDownloaderType

    /**
     * This method should be used to verify that the download file Hash matches the
     * passed in Hash returned by the server for the content.
     * This method is called on a background thread.
     * By default this method tires verify using the MD5 hash. If overriding this method,
     * also override getContentHash(responseHeaders: MutableMap<String, List<String>>) method.
     * @param request the request information for the download.
     * @param hash Hash returned by the server for the content
     * @return return true if the hash values match otherwise false. If false is returned,
     * this indicates that the download files is not correct so the download fails.
     * */
    fun verifyContentHash(request: ServerRequest, hash: String): Boolean

    /**
     * Get the content hash from Server.
     * By default this method returns the MD5 hash returned by the server response or an empty string
     * if the MD5 is not present. If overriding this method, also override the
     * verifyContentHash(request: ServerRequest, hash: String): Boolean method.
     * @param responseHeaders List of headers from response
     * @return return hash value returned by the server for the content. If hash information not found,
     * return empty string.
     * */
    fun getContentHash(responseHeaders: MutableMap<String, List<String>>): String

    /**
     * Notifies the downloader of the server response for a request. This method is called on a background thread.
     * @param request The request information for the download.
     * @param response Response containing the server response code, headers, connection success, content-length,
     * and input stream if a connection was successful.
     * */
    fun onServerResponse(request: ServerRequest, response: Response)

    /** Checks with the downloader to see if the HEAD Request Method is supported by the server.
     * If not, GET Request Method will be used to get information from the server. Default is true.
     * This method is called on a background thread.
     * @param request The request information for the download.
     * @return true if HEAD Request Method is supported. Otherwise false
     * */
    fun getHeadRequestMethodSupported(request: ServerRequest): Boolean

    /**
     * Attempts to get the ContentLength for a file located at the specified url.
     * This method runs on the calling thread.
     * @param request The request information for the download.
     * @return ContentLength if successful, or -1 if failed.
     * */
    fun getRequestContentLength(request: ServerRequest): Long

    /**
     * Attempts to get the buffer size for a specific download.
     * This method runs on a background thread. Note Android Framework may limit
     * the buffer size of io streams. So a very large buffer size may be limited to 8192
     * by the framework.
     * @param request The request information for the download.
     * @return buffer size or null. If the buffer size is not set. The default
     * buffer size will be 8192 bytes. Can be null.
     * */
    fun getRequestBufferSize(request: ServerRequest): Int

    /**
     * Gets a set of supported FileDownloaderTypes for a request.
     * @param request The request information for the download.
     * @return set of supported FileDownloaderTypes
     * */
    fun getRequestSupportedFileDownloaderTypes(request: ServerRequest): Set<FileDownloaderType>

    /**
     * A class that contains the information used by the Downloader to create a connection
     * to the server.
     * */
    open class ServerRequest(
            /** The request id.*/
            val id: Int,

            /** The url where the file will be downloaded from.*/
            val url: String,

            /** The headers used by the downloader to send header information to
             * the server about a request.*/
            val headers: Map<String, String>,

            /** The file where the download will be stored.*/
            val file: String,

            /** The file uri*/
            val fileUri: Uri,

            /** The tag associated with this request.*/
            val tag: String?,

            /** The identifier associated with this request*/
            val identifier: Long,

            /** Request Method. GET, HEAD or POST*/
            val requestMethod: String,

            /** The extras associated with this request*/
            val extras: Extras,

            /** If the original url was redirected.*/
            val redirected: Boolean,

            /** redirect url*/
            val redirectUrl: String,
            
            /**
             * If the request will be downloaded using the parallel download and the file slice count is greater the 1. This field will indicate
             * the request segment/part.
             * */
            val segment: Int)

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
            val request: ServerRequest,

            /** The file hash value to verify against.*/
            val hash: String,

            /** Server Response Headers */
            val responseHeaders: Map<String, List<String>>,

            /** Details if the server accepts byte ranges*/
            val acceptsRanges: Boolean,

            /** Error Response string. May be null*/
            val errorResponse: String?)

    /** File Downloading Type used to download each request.*/
    enum class FileDownloaderType {

        /** Performs the download sequentially. Bytes are downloaded in sequence.*/
        SEQUENTIAL,

        /** Performs the download by splitting parts of the file in parallel for download.
         * Fastest download option*/
        PARALLEL
    }

}