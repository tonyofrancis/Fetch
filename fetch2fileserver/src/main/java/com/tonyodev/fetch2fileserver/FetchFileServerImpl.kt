package com.tonyodev.fetch2fileserver

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.tonyodev.fetch2.FetchLogger
import com.tonyodev.fetch2.util.InterruptMonitor
import com.tonyodev.fetch2fileserver.database.FetchContentFileDatabase
import com.tonyodev.fetch2fileserver.provider.ContentFileProvider
import com.tonyodev.fetch2fileserver.provider.ContentFileProviderDelegate
import com.tonyodev.fetch2fileserver.provider.FetchContentFileProvider
import com.tonyodev.fetch2fileserver.transporter.ContentFileTransporterWriter
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*


class FetchFileServerImpl(context: Context,
                          private val serverSocket: ServerSocket,
                          private val clearContentFileDatabaseOnShutdown: Boolean,
                          private val logger: FetchLogger,
                          databaseName: String) : FetchFileServer {

    private val lock = Any()
    private val contentFileProviderMap = Collections.synchronizedMap(mutableMapOf<UUID, ContentFileProvider>())
    @Volatile
    private var isTerminated = false
    override var id: String = "FetchFileServer - " + UUID.randomUUID().toString()
    private var isForcedTerminated = false
    private var isStarted = false
    private var fetchFileServerAuthenticator: FetchFileServerAuthenticator? = null
    private var fetchFileServerDelegate: FetchFileServerDelegate? = null
    private var fetchTransferProgressListener: FetchTransferProgressListener? = null
    private val contentFileServerDatabase = FetchContentFileDatabase(context.applicationContext, databaseName)
    private val ioHandler = {
        val handlerThread = HandlerThread(id)
        handlerThread.start()
        Handler(handlerThread.looper)
    }()
    private val mainHandler = Handler(Looper.getMainLooper())

    override val port: Int
        get() {
            return synchronized(lock) {
                if (!isTerminated) {
                    serverSocket.localPort
                } else {
                    0
                }
            }
        }

    override val address: String
        get() {
            return synchronized(lock) {
                if (!isTerminated) {
                    serverSocket.inetAddress.hostAddress
                } else {
                    "00:00:00:00"
                }
            }
        }

    override val isShutDown: Boolean
        get() {
            return synchronized(lock) {
                isTerminated
            }
        }

    override fun start() {
        synchronized(lock) {
            throwIfTerminated()
            if (!isStarted && !isTerminated) {
                isStarted = true
                Thread({
                    while (!isTerminated) {
                        try {
                            val client = serverSocket.accept()
                            if (!isTerminated) {
                                processClient(client)
                            } else {
                                try {
                                    client.close()
                                } catch (e: Exception) {
                                    logger.e("FetchContentFileServer - ${e.message}")
                                }
                            }
                        } catch (e: Exception) {
                            logger.e("FetchContentFileServer - ${e.message}")
                        }
                    }
                    cleanUpServer()
                }).start()
            }
        }
    }

    private fun processClient(clientSocket: Socket) {
        if (!isTerminated) {
            val contentFileProvider = FetchContentFileProvider(clientSocket, contentFileProviderDelegate, logger, ioHandler)
            try {
                contentFileProviderMap[contentFileProvider.id] = contentFileProvider
                contentFileProvider.execute()
            } catch (e: Exception) {
                logger.e("FetchFileServer - ${e.message}")
                contentFileProviderDelegate.onFinished(contentFileProvider.id)
            }
        }
    }

    private val contentFileProviderDelegate = object : ContentFileProviderDelegate {

        override fun onFinished(id: UUID) {
            try {
                contentFileProviderMap.remove(id)
            } catch (e: Exception) {
                logger.e("FetchFileServer - ${e.message}")
            }
        }

        override fun getContentFile(contentFileIdentifier: String): ContentFile? {
            return synchronized(lock) {
                try {
                    val id: Long = contentFileIdentifier.toLong()
                    if (id == FileRequest.CATALOG_ID) {
                        val catalog = contentFileServerDatabase.getRequestedCatalog(-1, -1)
                        val catalogContentFile = ContentFile()
                        catalogContentFile.id = FileRequest.CATALOG_ID
                        catalogContentFile.customData = catalog
                        catalogContentFile.name = "Catalog.json"
                        catalogContentFile.file = "/Catalog.json"
                        catalogContentFile
                    } else {
                        contentFileServerDatabase.get(id)
                    }
                } catch (e: Exception) {
                    contentFileServerDatabase.get(contentFileIdentifier)
                }
            }
        }

        override fun acceptAuthorization(authorization: String, fileRequest: FileRequest): Boolean {
            return fetchFileServerAuthenticator?.accept(authorization, fileRequest) ?: true
        }

        override fun onClientConnected(client: String, fileRequest: FileRequest) {
            mainHandler.post {
                fetchFileServerDelegate?.onClientConnected(client, fileRequest)
            }
        }

        override fun onClientDidProvideCustomData(client: String, customData: String, fileRequest: FileRequest) {
            mainHandler.post {
                fetchFileServerDelegate?.onClientDidProvideCustomData(client, customData, fileRequest)
            }
        }

        override fun onClientDisconnected(client: String) {
            mainHandler.post {
                fetchFileServerDelegate?.onClientDisconnected(client)
            }
        }

        override fun getCatalog(page: Int, size: Int): String {
            return contentFileServerDatabase.getRequestedCatalog(page, size)
        }

        override fun getFileInputStream(contentFile: ContentFile, fileOffset: Long): InputStream? {
            return fetchFileServerDelegate?.getFileInputStream(contentFile, fileOffset)
        }

        override fun onProgress(client: String, contentFile: ContentFile, progress: Int) {
            mainHandler.post {
                fetchTransferProgressListener?.onProgress(client, contentFile, progress)
            }
        }

        override fun onCustomRequest(client: String, fileRequest: FileRequest,
                                     contentFileTransporterWriter: ContentFileTransporterWriter, interruptMonitor: InterruptMonitor) {
            fetchFileServerDelegate?.onCustomRequest(client, fileRequest,
                    contentFileTransporterWriter, interruptMonitor)
        }

    }

    private fun cleanUpServer() {
        try {
            serverSocket.close()
        } catch (e: Exception) {
            logger.e("FetchFileServer - ${e.message}")
        }
        try {
            contentFileProviderMap.clear()
        } catch (e: Exception) {
            logger.e("FetchFileServer - ${e.message}")
        }
        try {
            if (clearContentFileDatabaseOnShutdown) {
                ioHandler.post {
                    contentFileServerDatabase.deleteAll()
                    try {
                        contentFileServerDatabase.close()
                    } catch (e: Exception) {
                        logger.e("FetchFileServer - ${e.message}")
                    }
                    try {
                        ioHandler.removeCallbacks(null)
                        ioHandler.looper.quit()
                    } catch (e: Exception) {
                        logger.e("FetchFileServer - ${e.message}")
                    }
                }
            } else {
                contentFileServerDatabase.close()
                try {
                    ioHandler.removeCallbacks(null)
                    ioHandler.looper.quit()
                } catch (e: Exception) {
                    logger.e("FetchFileServer - ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.e("FetchFileServer - ${e.message}")
        }
        isStarted = false
    }

    override fun shutDown(forced: Boolean) {
        synchronized(lock) {
            if (!isTerminated) {
                isTerminated = true
                isForcedTerminated = forced
                interruptAllProviders()
                if (isForcedTerminated) {
                    forceShutdown()
                }
            }
        }
    }

    private fun forceShutdown() {
        cleanUpServer()
    }

    private fun interruptAllProviders() {
        try {
            val iterator = contentFileProviderMap.values.iterator()
            while (iterator.hasNext()) {
                iterator.next().interrupt()
            }
            if (contentFileProviderMap.isEmpty()) {
                cleanUpServer()
            }
        } catch (e: Exception) {
            logger.e("FetchFileServer - ${e.message}")
        }
    }

    override fun addContentFile(contentFile: ContentFile) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                contentFileServerDatabase.insert(contentFile)
            }
        }
    }

    override fun addContentFiles(contentFiles: Collection<ContentFile>) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                contentFileServerDatabase.insert(contentFiles.toList())
            }
        }
    }

    override fun removeContentFile(contentFile: ContentFile) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                contentFileServerDatabase.delete(contentFile)
            }
        }
    }

    override fun removeAllContentFiles() {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                contentFileServerDatabase.deleteAll()
            }
        }
    }

    override fun removeContentFiles(contentFiles: Collection<ContentFile>) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                contentFileServerDatabase.delete(contentFiles.toList())
            }
        }
    }

    override fun setAuthenticator(fetchFileServerAuthenticator: FetchFileServerAuthenticator?) {
        synchronized(lock) {
            this.fetchFileServerAuthenticator = fetchFileServerAuthenticator
        }
    }

    override fun setDelegate(fetchFileServerDelegate: FetchFileServerDelegate?) {
        synchronized(lock) {
            this.fetchFileServerDelegate = fetchFileServerDelegate
        }
    }

    override fun getContentFiles(callback: (List<ContentFile>) -> Unit) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                val contentFiles = contentFileServerDatabase.get()
                mainHandler.post {
                    callback(contentFiles)
                }
            }
        }
    }

    override fun containsContentFile(contentId: Int, callback: (Boolean) -> Unit) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                val contentFile = contentFileServerDatabase.get(id)
                mainHandler.post {
                    if (contentFile != null) {
                        callback(true)
                    } else {
                        callback(false)
                    }
                }
            }
        }
    }

    override fun getContentFile(contentId: Int, callback: (ContentFile?) -> Unit) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                val contentFile = contentFileServerDatabase.get(id)
                mainHandler.post {
                    callback(contentFile)
                }
            }
        }
    }

    override fun getFullCatalog(callback: (String) -> Unit) {
        synchronized(lock) {
            throwIfTerminated()
            val catalog = contentFileServerDatabase.getRequestedCatalog(-1, -1)
            mainHandler.post {
                callback(catalog)
            }
        }
    }

    override fun setTransferProgressListener(fetchTransferProgressListener: FetchTransferProgressListener?) {
        synchronized(lock) {
            this.fetchTransferProgressListener = fetchTransferProgressListener
        }
    }

    private fun throwIfTerminated() {
        if (isTerminated) {
            throw Exception("FetchFileServer was already Shutdown. It cannot be restarted. Get a new Instance.")
        }
    }


}