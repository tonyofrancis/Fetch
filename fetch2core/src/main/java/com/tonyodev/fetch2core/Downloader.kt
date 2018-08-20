package com.tonyodev.fetch2core

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
interface Downloader : Closeable {

    /**
     * This method is called by Fetch to execute a request against the client.
     * This method is responsible for creating and opening a connection then returning
     * the response back to the Fetch FileDownloader for processing. This method
     * is called on a background thread.
     * @param request The request information for the download.
     * @param interruptMonitor Notifies the downloader that there may be an interruption for the request.
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
     * This method is called by Fetch to request the OutputResourceWrapper that will be used to save
     * the download information too. If null is returned, Fetch will provide the OutputResourceWrapper.
     * This method is called on a background thread.
     * @param request The request information for the download.
     * @return OutputResourceWrapper object. Fetch will call the close method automatically
     *         after the disconnect(response) method is called. Can return null. If null,
     *         Fetch will provide the OutputResourceWrapper.
     * */
    fun getRequestOutputResourceWrapper(request: ServerRequest): OutputResourceWrapper?

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
     * This method is called by Fetch for download requests that are downloading using the
     * FileDownloaderType.PARALLEL type. Fetch uses this directory to store the
     * temp files for the request. If the return directory is null, Fetch
     * will select the default directory. Temp files in this directory are automatically
     * deleted by Fetch once a download completes or a request is removed.
     * This method is called on a background thread.
     * @param request the request information for the download.
     * @return the directory where the temp files will be stored. Can be null.
     * */
    fun getDirectoryForFileDownloaderTypeParallel(request: ServerRequest): String?

    /**
     * This method should be used to verify that the download file MD5 matches the
     * passed in MD5 returned by the server for the content.
     * This method is called on a background thread.
     * @param request the request information for the download.
     * @param md5 MD5 returned by the server for the content
     * @return return true if the md5 values match otherwise false. If false is returned,
     * this indicates that the download files is not correct so the download fails.
     * */
    fun verifyContentMD5(request: ServerRequest, md5: String): Boolean

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

            /** The tag associated with this request.*/
            val tag: String?,

            /** The identifier associated with this request*/
            val identifier: Long,

            /** Request Method. GET, HEAD or POST*/
            val requestMethod: String,

            /** The extras associated with this request*/
            val extras: Extras)

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

            /** The file md5 value to verify against.*/
            val md5: String,

            /** Server Response Headers */
            val responseHeaders: Map<String, List<String>>,

            /** Details if the server accepts byte ranges*/
            val acceptsRanges: Boolean)

    /** File Downloading Type used to download each request.*/
    enum class FileDownloaderType {

        /** Performs the download sequentially. Bytes are downloaded in sequence.*/
        SEQUENTIAL,

        /** Performs the download by splitting parts of the file in parallel for download.
         * Fastest download option*/
        PARALLEL
    }

}