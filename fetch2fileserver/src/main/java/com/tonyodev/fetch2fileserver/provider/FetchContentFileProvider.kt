package com.tonyodev.fetch2fileserver.provider

import android.os.Handler
import com.tonyodev.fetch2.FetchLogger
import com.tonyodev.fetch2.util.*
import com.tonyodev.fetch2fileserver.ContentFileRequest
import com.tonyodev.fetch2fileserver.ContentFileResponse
import com.tonyodev.fetch2fileserver.ContentFileResponse.Companion.CLOSE_CONNECTION
import com.tonyodev.fetch2fileserver.ContentFileResponse.Companion.OPEN_CONNECTION
import com.tonyodev.fetch2fileserver.transporter.ContentFileTransporter
import com.tonyodev.fetch2fileserver.transporter.FetchContentFileTransporter
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.Socket
import java.util.*


class FetchContentFileProvider(private val client: Socket,
                               private val contentFileProviderDelegate: ContentFileProviderDelegate,
                               private val logger: FetchLogger,
                               private val ioHandler: Handler) : ContentFileProvider {

    private val lock = Any()
    val id = UUID.randomUUID()
    private val transporter: ContentFileTransporter = FetchContentFileTransporter(client)
    @Volatile
    private var interrupted: Boolean = false
    private var fileInputStream: InputStream? = null
    private var randomAccessFile: RandomAccessFile? = null
    private var clientRequest: ContentFileRequest? = null
    private var persistConnection = true
    private val persistentRunnable = Runnable {
        interrupted = true
    }

    override fun execute() {
        Thread({
            try {
                while (persistConnection && !interrupted) {
                    ioHandler.postDelayed(persistentRunnable, 600000)
                    clientRequest = getContentFileClientRequest()
                    ioHandler.removeCallbacks(persistentRunnable)
                    val request = clientRequest
                    if (request != null && !interrupted) {
                        persistConnection = request.persistConnection
                        if (!interrupted && contentFileProviderDelegate.acceptAuthorization(request.authorization, request)) {
                            logger.d("ContentFileProvider - ClientRequestAccepted - ${request.toJsonString}")
                            logger.d("ContentFileProvider - Client Connected - $client")
                            contentFileProviderDelegate.onClientConnected(request.client, request)
                            contentFileProviderDelegate.onClientDidProvideCustomData(request.client, request.customData, request)
                            when (request.type) {
                                ContentFileRequest.TYPE_PING -> {
                                    if (!interrupted) {
                                        sendPingResponse()
                                    }
                                }
                                ContentFileRequest.TYPE_CATALOG -> {
                                    if (!interrupted) {
                                        val catalog = contentFileProviderDelegate.getCatalog(request.page, request.size)
                                        val data = catalog.toByteArray(Charsets.UTF_8)
                                        if (!interrupted) {
                                            val contentLength = (if (request.rangeEnd == -1L) data.size.toLong() else request.rangeEnd) - request.rangeStart
                                            sendCatalogResponse(contentLength, getMd5String(data))
                                            transporter.sendRawBytes(data, request.rangeStart.toInt(), contentLength.toInt())
                                        }
                                    }
                                }
                                ContentFileRequest.TYPE_FILE -> {
                                    val contentFile = contentFileProviderDelegate.getContentFile(request.contentFileId)
                                    if (!interrupted) {
                                        if (contentFile != null) {
                                            fileInputStream = contentFileProviderDelegate.getFileInputStream(contentFile, request.rangeStart)
                                            if (fileInputStream == null) {
                                                if (contentFile.id == ContentFileRequest.CATALOG_ID) {
                                                    val catalog = contentFile.customData.toByteArray(Charsets.UTF_8)
                                                    contentFile.length = if (request.rangeEnd == -1L) catalog.size.toLong() else request.rangeEnd
                                                    contentFile.md5 = getMd5String(catalog)
                                                    fileInputStream = ByteArrayInputStream(catalog, request.rangeStart.toInt(), contentFile.length.toInt())
                                                } else {
                                                    randomAccessFile = RandomAccessFile(contentFile.file, "r")
                                                    randomAccessFile?.seek(request.rangeStart)
                                                }
                                            }
                                            if (!interrupted) {
                                                var reportingStopTime: Long
                                                val byteArray = ByteArray(ContentFileTransporter.BUFFER_SIZE)
                                                val contentLength = (if (request.rangeEnd == -1L) contentFile.length else request.rangeEnd) - request.rangeStart
                                                var remainderBytes = contentLength
                                                sendContentFileResponse(contentLength, contentFile.md5)
                                                var reportingStartTime = System.nanoTime()
                                                var read = (fileInputStream?.read(byteArray)
                                                        ?: randomAccessFile?.read(byteArray)) ?: -1
                                                var streamBytes: Int
                                                while (remainderBytes > 0L && read != -1 && !interrupted) {
                                                    streamBytes = if (read <= remainderBytes) {
                                                        read
                                                    } else {
                                                        read = -1
                                                        remainderBytes.toInt()
                                                    }
                                                    transporter.sendRawBytes(byteArray, 0, streamBytes)
                                                    if (read != -1) {
                                                        remainderBytes -= streamBytes
                                                        reportingStopTime = System.nanoTime()
                                                        val hasReportingTimeElapsed = hasIntervalTimeElapsed(reportingStartTime,
                                                                reportingStopTime, DEFAULT_PROGRESS_REPORTING_INTERVAL_IN_MILLISECONDS)
                                                        if (hasReportingTimeElapsed && !interrupted) {
                                                            val progress = calculateProgress(contentLength - remainderBytes, contentLength)
                                                            contentFileProviderDelegate.onProgress(request.client, contentFile, progress)
                                                            reportingStartTime = System.nanoTime()
                                                        }
                                                        read = (fileInputStream?.read(byteArray)
                                                                ?: randomAccessFile?.read(byteArray)) ?: -1
                                                    }
                                                }
                                                if (remainderBytes == 0L && !interrupted) {
                                                    contentFileProviderDelegate.onProgress(request.client, contentFile, 100)
                                                }
                                            }
                                            cleanFileStreams()
                                        } else {
                                            sendInvalidResponse(HttpURLConnection.HTTP_NO_CONTENT)
                                        }
                                    }
                                }
                                ContentFileRequest.TYPE_INVALID -> {

                                }
                                else -> {
                                    if (!interrupted) {
                                        contentFileProviderDelegate.onCustomRequest(request.client, request, transporter, interruptMonitor)
                                    }
                                }
                            }
                            logger.d("ContentFileProvider - Client Disconnected - $client")
                            contentFileProviderDelegate.onClientDisconnected(request.client)
                        } else if (!interrupted) {
                            logger.d("ContentFileProvider - ClientRequestRejected - ${request.toJsonString}")
                            sendInvalidResponse(HttpURLConnection.HTTP_FORBIDDEN)
                        }
                    } else if (!interrupted) {
                        sendInvalidResponse(HttpURLConnection.HTTP_BAD_REQUEST)
                    }
                    clientRequest = null
                }
            } catch (e: Exception) {
                logger.e("ContentFileProvider - ${e.message}")
                try {
                    sendInvalidResponse(HttpURLConnection.HTTP_INTERNAL_ERROR)
                } catch (e: Exception) {
                    logger.e("ContentFileProvider - ${e.message}")
                }
            } finally {
                if (interrupted) {
                    try {
                        sendInvalidResponse(HttpURLConnection.HTTP_INTERNAL_ERROR)
                    } catch (e: Exception) {
                        logger.e("ContentFileProvider - ${e.message}")
                    }
                }
                transporter.close()
                cleanFileStreams()
                try {
                    contentFileProviderDelegate.onFinished(id)
                } catch (e: Exception) {
                    logger.e("ContentFileProvider - ${e.message}")
                }
            }
        }).start()
    }

    private fun cleanFileStreams() {
        try {
            fileInputStream?.close()
        } catch (e: Exception) {
            logger.e("ContentFileProvider - ${e.message}")
        }
        try {
            randomAccessFile?.close()
        } catch (e: Exception) {
            logger.e("ContentFileProvider - ${e.message}")
        }
        fileInputStream = null
        randomAccessFile = null
    }

    private val interruptMonitor = object : InterruptMonitor {
        override val isInterrupted: Boolean
            get() {
                return interrupted
            }
    }

    private fun getContentFileClientRequest(): ContentFileRequest? {
        while (!interrupted) {
            val request = transporter.receiveContentFileRequest()
            if (request != null) {
                return request
            }
        }
        return null
    }

    private fun sendPingResponse() {
        val response = ContentFileResponse(status = HttpURLConnection.HTTP_OK,
                type = clientRequest?.type ?: ContentFileRequest.TYPE_PING,
                connection = OPEN_CONNECTION,
                date = Date().time,
                contentLength = 0)
        transporter.sendContentFileResponse(response)
    }

    private fun sendInvalidResponse(status: Int) {
        val response = ContentFileResponse(status = status,
                type = clientRequest?.type ?: ContentFileRequest.TYPE_INVALID,
                connection = CLOSE_CONNECTION,
                date = Date().time,
                contentLength = 0)
        transporter.sendContentFileResponse(response)
        interrupted = true
    }

    private fun sendCatalogResponse(contentLength: Long, md5: String) {
        val response = ContentFileResponse(status = HttpURLConnection.HTTP_OK,
                type = clientRequest?.type ?: ContentFileRequest.TYPE_CATALOG,
                connection = OPEN_CONNECTION,
                date = Date().time,
                contentLength = contentLength,
                md5 = md5)
        transporter.sendContentFileResponse(response)
    }

    private fun sendContentFileResponse(contentLength: Long, md5: String) {
        val response = ContentFileResponse(status = HttpURLConnection.HTTP_PARTIAL,
                type = clientRequest?.type ?: ContentFileRequest.TYPE_FILE,
                connection = OPEN_CONNECTION,
                date = Date().time,
                contentLength = contentLength,
                md5 = md5)
        transporter.sendContentFileResponse(response)
    }

    override fun interrupt() {
        synchronized(lock) {
            if (!interrupted) {
                interrupted = true
            }
        }
    }

}