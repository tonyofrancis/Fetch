package com.tonyodev.fetch2fileserver.provider

import android.os.Handler
import com.tonyodev.fetch2core.*
import com.tonyodev.fetch2fileserver.database.toFileResource
import com.tonyodev.fetch2core.transporter.FileRequest
import com.tonyodev.fetch2core.transporter.FileResponse
import com.tonyodev.fetch2core.transporter.FileResponse.Companion.CLOSE_CONNECTION
import com.tonyodev.fetch2core.transporter.FileResponse.Companion.OPEN_CONNECTION
import com.tonyodev.fetch2core.transporter.FileResourceTransporter
import com.tonyodev.fetch2core.transporter.FetchFileResourceTransporter
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.Socket
import java.util.*


class FetchFileResourceProvider(private val client: Socket,
                                private val fileResourceProviderDelegate: FileResourceProviderDelegate,
                                private val logger: FetchLogger,
                                private val ioHandler: Handler) : FileResourceProvider {

    private val lock = Any()
    val id = UUID.randomUUID()
    private val transporter: FileResourceTransporter = FetchFileResourceTransporter(client)
    @Volatile
    private var interrupted: Boolean = false
    private var fileInputStream: InputStream? = null
    private var randomAccessFile: RandomAccessFile? = null
    private var clientRequest: FileRequest? = null
    private var persistConnection = true
    private val persistentRunnable = Runnable {
        interrupted = true
    }

    override fun execute() {
        Thread {
            try {
                while (persistConnection && !interrupted) {
                    ioHandler.postDelayed(persistentRunnable, 600000)
                    clientRequest = getFileResourceClientRequest()
                    ioHandler.removeCallbacks(persistentRunnable)
                    val request = clientRequest
                    if (request != null && !interrupted) {
                        persistConnection = request.persistConnection
                        if (!interrupted && fileResourceProviderDelegate.acceptAuthorization(request.authorization, request)) {
                            logger.d("FetchFileServerProvider - ClientRequestAccepted - ${request.toJsonString}")
                            logger.d("FetchFileServerProvider - Client Connected - $client")
                            fileResourceProviderDelegate.onClientConnected(request.client, request)
                            fileResourceProviderDelegate.onClientDidProvideCustomData(request.client, request.customData, request)
                            when (request.type) {
                                FileRequest.TYPE_PING -> {
                                    if (!interrupted) {
                                        sendPingResponse()
                                    }
                                }
                                FileRequest.TYPE_CATALOG -> {
                                    if (!interrupted) {
                                        val catalog = fileResourceProviderDelegate.getCatalog(request.page, request.size)
                                        val data = catalog.toByteArray(Charsets.UTF_8)
                                        if (!interrupted) {
                                            val contentLength = (if (request.rangeEnd == -1L) data.size.toLong() else request.rangeEnd) - request.rangeStart
                                            sendCatalogResponse(contentLength, getMd5String(data))
                                            transporter.sendRawBytes(data, request.rangeStart.toInt(), contentLength.toInt())
                                        }
                                    }
                                }
                                FileRequest.TYPE_FILE -> {
                                    val fileResourceInfo = fileResourceProviderDelegate.getFileResource(request.fileResourceId)
                                    if (!interrupted) {
                                        if (fileResourceInfo != null) {
                                            val fileResource = fileResourceInfo.toFileResource()
                                            fileInputStream = fileResourceProviderDelegate.getFileInputStream(fileResource, request.rangeStart)
                                            if (fileInputStream == null) {
                                                if (fileResourceInfo.id == FileRequest.CATALOG_ID) {
                                                    val catalog = fileResourceInfo.customData.toByteArray(Charsets.UTF_8)
                                                    fileResourceInfo.length = if (request.rangeEnd == -1L) catalog.size.toLong() else request.rangeEnd
                                                    fileResourceInfo.md5 = getMd5String(catalog)
                                                    fileInputStream = ByteArrayInputStream(catalog, request.rangeStart.toInt(), fileResourceInfo.length.toInt())
                                                } else {
                                                    randomAccessFile = RandomAccessFile(fileResourceInfo.file, "r")
                                                    randomAccessFile?.seek(request.rangeStart)
                                                }
                                            }
                                            if (!interrupted) {
                                                var reportingStopTime: Long
                                                val byteArray = ByteArray(FileResourceTransporter.BUFFER_SIZE)
                                                val contentLength = (if (request.rangeEnd == -1L) fileResourceInfo.length else request.rangeEnd) - request.rangeStart
                                                var remainderBytes = contentLength
                                                sendFileResourceResponse(contentLength, fileResourceInfo.md5)
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
                                                            fileResourceProviderDelegate.onProgress(request.client, fileResource, progress)
                                                            reportingStartTime = System.nanoTime()
                                                        }
                                                        read = (fileInputStream?.read(byteArray)
                                                                ?: randomAccessFile?.read(byteArray)) ?: -1
                                                    }
                                                }
                                                if (remainderBytes == 0L && !interrupted) {
                                                    fileResourceProviderDelegate.onProgress(request.client, fileResource, 100)
                                                }
                                            }
                                            cleanFileStreams()
                                        } else {
                                            sendInvalidResponse(HttpURLConnection.HTTP_NO_CONTENT)
                                        }
                                    }
                                }
                                FileRequest.TYPE_INVALID -> {

                                }
                                else -> {
                                    if (!interrupted) {
                                        fileResourceProviderDelegate.onCustomRequest(request.client, request, transporter, interruptMonitor)
                                    }
                                }
                            }
                            logger.d("FetchFileServerProvider - Client Disconnected - $client")
                            fileResourceProviderDelegate.onClientDisconnected(request.client)
                        } else if (!interrupted) {
                            logger.d("FetchFileServerProvider - ClientRequestRejected - ${request.toJsonString}")
                            sendInvalidResponse(HttpURLConnection.HTTP_FORBIDDEN)
                        }
                    } else if (!interrupted) {
                        sendInvalidResponse(HttpURLConnection.HTTP_BAD_REQUEST)
                    }
                    clientRequest = null
                }
            } catch (e: Exception) {
                logger.e("FetchFileServerProvider - ${e.message}")
                try {
                    sendInvalidResponse(HttpURLConnection.HTTP_INTERNAL_ERROR)
                } catch (e: Exception) {
                    logger.e("FetchFileServerProvider - ${e.message}")
                }
            } finally {
                if (interrupted) {
                    try {
                        sendInvalidResponse(HttpURLConnection.HTTP_INTERNAL_ERROR)
                    } catch (e: Exception) {
                        logger.e("FetchFileServerProvider - ${e.message}")
                    }
                }
                transporter.close()
                cleanFileStreams()
                try {
                    fileResourceProviderDelegate.onFinished(id)
                } catch (e: Exception) {
                    logger.e("FetchFileServerProvider - ${e.message}")
                }
            }
        }.start()
    }

    private fun cleanFileStreams() {
        try {
            fileInputStream?.close()
        } catch (e: Exception) {
            logger.e("FetchFileServerProvider - ${e.message}")
        }
        try {
            randomAccessFile?.close()
        } catch (e: Exception) {
            logger.e("FetchFileServerProvider - ${e.message}")
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

    private fun getFileResourceClientRequest(): FileRequest? {
        while (!interrupted) {
            val request = transporter.receiveFileRequest()
            if (request != null) {
                return request
            }
        }
        return null
    }

    private fun sendPingResponse() {
        val response = FileResponse(status = HttpURLConnection.HTTP_OK,
                type = clientRequest?.type
                        ?: FileRequest.TYPE_PING,
                connection = OPEN_CONNECTION,
                date = Date().time,
                contentLength = 0)
        transporter.sendFileResponse(response)
    }

    private fun sendInvalidResponse(status: Int) {
        val response = FileResponse(status = status,
                type = clientRequest?.type
                        ?: FileRequest.TYPE_INVALID,
                connection = CLOSE_CONNECTION,
                date = Date().time,
                contentLength = 0)
        transporter.sendFileResponse(response)
        interrupted = true
    }

    private fun sendCatalogResponse(contentLength: Long, md5: String) {
        val response = FileResponse(status = HttpURLConnection.HTTP_OK,
                type = clientRequest?.type
                        ?: FileRequest.TYPE_CATALOG,
                connection = OPEN_CONNECTION,
                date = Date().time,
                contentLength = contentLength,
                md5 = md5)
        transporter.sendFileResponse(response)
    }

    private fun sendFileResourceResponse(contentLength: Long, md5: String) {
        val response = FileResponse(status = HttpURLConnection.HTTP_PARTIAL,
                type = clientRequest?.type
                        ?: FileRequest.TYPE_FILE,
                connection = OPEN_CONNECTION,
                date = Date().time,
                contentLength = contentLength,
                md5 = md5)
        transporter.sendFileResponse(response)
    }

    override fun interrupt() {
        synchronized(lock) {
            if (!interrupted) {
                interrupted = true
            }
        }
    }

}