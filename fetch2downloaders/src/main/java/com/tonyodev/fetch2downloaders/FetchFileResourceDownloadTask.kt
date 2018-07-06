package com.tonyodev.fetch2downloaders

import android.os.Handler
import android.os.Looper
import com.tonyodev.fetch2core.*
import com.tonyodev.fetch2core.transporter.FetchFileResourceTransporter
import com.tonyodev.fetch2core.transporter.FileRequest
import com.tonyodev.fetch2core.transporter.FileResponse
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.util.*

/** Downloader Task used to download a File/FileResource from a Fetch File Server.*/
abstract class FetchFileResourceDownloadTask<T> {

    /** Task identifier.*/
    val id = UUID.randomUUID().toString()

    /** Called by the task to get the FileResourceRequest used to make the connection between the client
     * and the Fetch File Server. Called on a background thread.
     * @return FileResourceRequest
     * */
    abstract fun getRequest(): FileResourceRequest

    /** Called by the task when the connection to the server is successfully established and
     * the task is ready to download the content. Called a background thread.
     * @param inputStream InputStream used to get the content.
     * @param contentLength the length of the content.
     * @param md5CheckSum the md5 check sum of the content
     * @return result.
     * */
    abstract fun doWork(inputStream: InputStream, contentLength: Long, md5CheckSum: String): T

    /** Called by the task to report progress. This method is called on the main thread.
     * @param progress progress value.
     * */
    protected open fun onProgress(progress: Int) {

    }

    /** Called by the task when an error occurs. The task is interrupted and stopped.
     * This method is called on the main thread.
     * @param httpStatusCode http status code. Can be null
     * @param throwable throwable when an exception is throw. Can be null
     * */
    protected open fun onError(httpStatusCode: Int?, throwable: Throwable? = null) {

    }

    /** Called by the task when the task completed. This method is called on the main thread.
     * @param result result returned by the doWork method.
     * */
    protected open fun onComplete(result: T) {

    }

    /** Executes a task. The task can be reused after it has stopped.*/
    fun execute() {
        synchronized(lock) {
            if (!isExecutingTask) {
                isExecutingTask = true
                Thread(taskRunnable).start()
            }
        }
    }

    /** cancels the task if its currently executing.*/
    fun cancel() {
        synchronized(lock) {
            if (isExecutingTask) {
                interrupted = true
            }
        }
    }

    /** Checks if a task is cancelled.*/
    val isCancelled: Boolean
        get() {
            synchronized(lock) {
                return interrupted
            }
        }

    /** Checks if a task is running.*/
    val isRunning: Boolean
        get() {
            synchronized(lock) {
                return isExecutingTask
            }
        }

    /** Sets the task progress and calls the onProgress method on the main thread
     * to post the progress update.*/
    protected fun setProgress(progress: Int) {
        mainHandler.post {
            onProgress(progress)
        }
    }

    /** Class used to make a connection between a client and Fetch file server.*/
    class FileResourceRequest(
            /** File server IP Address*/
            var hostAddress: String = "00:00:00:00",
            /** File server port. The port the file server is listening for requests on.*/
            var port: Int = 0,
            /** FileResource identifier. This can be the FileResource name or id.*/
            var resourceIdentifier: String = "",
            /** header information for the request.*/
            var headers: MutableMap<String, String> = mutableMapOf()) {

        /** Adds a header to the request.
         * @param key header key
         * @param value header value
         * */
        fun addHeader(key: String, value: String) {
            headers[key] = value
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FileResourceRequest
            if (hostAddress != other.hostAddress) return false
            if (port != other.port) return false
            if (resourceIdentifier != other.resourceIdentifier) return false
            if (headers != other.headers) return false
            return true
        }

        override fun hashCode(): Int {
            var result = hostAddress.hashCode()
            result = 31 * result + port
            result = 31 * result + resourceIdentifier.hashCode()
            result = 31 * result + headers.hashCode()
            return result
        }

        override fun toString(): String {
            return "FileResourceRequest(hostAddress='$hostAddress', port=$port, " +
                    "resourceIdentifier='$resourceIdentifier', headers=$headers)"
        }

    }

    /******* Internal ********/

    private val lock = Any()
    @Volatile
    private var interrupted = false
    @Volatile
    private var isExecutingTask = false
    private lateinit var transporter: FetchFileResourceTransporter
    private val mainHandler = Handler(Looper.getMainLooper())

    private val taskRunnable = Runnable {
        try {
            val serverRequest = getRequest()
            val url = FetchFileServerUrlBuilder()
                    .setHostInetAddress(serverRequest.hostAddress, serverRequest.port)
                    .setFileResourceIdentifier(serverRequest.resourceIdentifier)
                    .create()
            val headers = serverRequest.headers
            val range = getRangeForFetchFileServerRequest(headers["Range"]
                    ?: "bytes=0-")
            val authorization = headers[FileRequest.FIELD_AUTHORIZATION] ?: ""
            val port = getFetchFileServerPort(url)
            val address = getFetchFileServerHostAddress(url)
            val inetSocketAddress = InetSocketAddress(address, port)
            transporter = FetchFileResourceTransporter()
            var timeoutStop: Long
            val timeoutStart = System.nanoTime()
            transporter.connect(inetSocketAddress)
            val fileRequest = FileRequest(
                    type = FileRequest.TYPE_FILE,
                    fileResourceId = getFileResourceIdFromUrl(url),
                    rangeStart = range.first,
                    rangeEnd = range.second,
                    authorization = authorization,
                    client = headers[FileRequest.FIELD_CLIENT]
                            ?: UUID.randomUUID().toString(),
                    customData = headers[FileRequest.FIELD_CUSTOM_DATA]
                            ?: "",
                    page = headers[FileRequest.FIELD_PAGE]?.toIntOrNull()
                            ?: 0,
                    size = headers[FileRequest.FIELD_SIZE]?.toIntOrNull()
                            ?: 0,
                    persistConnection = false)
            transporter.sendFileRequest(fileRequest)
            while (!interrupted) {
                val serverResponse = transporter.receiveFileResponse()
                if (serverResponse != null) {
                    val isSuccessful = serverResponse.connection == FileResponse.OPEN_CONNECTION &&
                            serverResponse.type == FileRequest.TYPE_FILE && serverResponse.status == HttpURLConnection.HTTP_PARTIAL
                    if (!isSuccessful) {
                        mainHandler.post {
                            onError(serverResponse.status)
                        }
                    } else {
                        val contentLength = serverResponse.contentLength
                        val inputStream = transporter.getInputStream()
                        val md5 = serverResponse.md5
                        val result = doWork(inputStream, contentLength, md5)
                        mainHandler.post {
                            onComplete(result)
                        }
                    }
                    break
                }
                timeoutStop = System.nanoTime()
                if (hasIntervalTimeElapsed(timeoutStart, timeoutStop, 20_000)) {
                    mainHandler.post {
                        onError(HttpURLConnection.HTTP_CLIENT_TIMEOUT)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            mainHandler.post {
                onError(null, e)
            }
        } finally {
            cleanUpTask()
        }
    }

    private fun cleanUpTask() {
        transporter.close()
        interrupted = false
        isExecutingTask = false
    }

}