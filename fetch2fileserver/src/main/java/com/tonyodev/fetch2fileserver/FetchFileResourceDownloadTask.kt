package com.tonyodev.fetch2fileserver

import android.os.Handler
import android.os.Looper
import com.tonyodev.fetch2core.*
import com.tonyodev.fetch2fileserver.transporter.FetchFileResourceTransporter
import com.tonyodev.fetch2fileserver.transporter.FileRequest
import com.tonyodev.fetch2fileserver.transporter.FileResponse
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.util.*

abstract class FetchFileResourceDownloadTask<T> @JvmOverloads constructor(val timeout: Long = 20_000) {

    abstract fun getRequest(): FileResourceRequest

    abstract fun doWork(inputStream: InputStream, contentLength: Long, md5CheckSum: String): T?

    abstract fun onError(httpStatusCode: Int, throwable: Throwable? = null)

    abstract fun onComplete(result: T?)


    fun execute() {
        synchronized(lock) {
            if (!isExecutingTask) {
                isExecutingTask = true
                Thread(taskRunnable).start()
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            if (isExecutingTask) {
                interrupted = true
            }
        }
    }

    protected val isInterrupted: Boolean
        get() {
            return interrupted
        }

    class FileResourceRequest(var hostAddress: String = "00:00:00:00",
                              var port: Int = 0,
                              var resourceIdentifier: String = "",
                              var headers: MutableMap<String, String> = mutableMapOf()) {

        fun addHeader(key: String, value: String) {
            headers[key] = value
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
            val range = getRangeForFetchFileServerRequest(headers["Range"] ?: "bytes=0-")
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
                if (hasIntervalTimeElapsed(timeoutStart, timeoutStop, timeout)) {
                    mainHandler.post {
                        onError(HttpURLConnection.HTTP_CLIENT_TIMEOUT)
                    }
                    break
                }
            }
            if (interrupted) {
                mainHandler.post {
                    onError(HttpURLConnection.HTTP_UNSUPPORTED_TYPE)
                }
            }
        } catch (e: Exception) {
            mainHandler.post {
                onError(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, e)
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