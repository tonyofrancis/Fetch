package com.tonyodev.fetch2fileserver

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.tonyodev.fetch2core.*

import com.tonyodev.fetch2fileserver.database.FetchFileResourceInfoDatabase
import com.tonyodev.fetch2fileserver.database.FileResourceInfo
import com.tonyodev.fetch2fileserver.database.toFileResource
import com.tonyodev.fetch2fileserver.database.toFileResourceInfo
import com.tonyodev.fetch2fileserver.provider.FileResourceProvider
import com.tonyodev.fetch2fileserver.provider.FileResourceProviderDelegate
import com.tonyodev.fetch2fileserver.provider.FetchFileResourceProvider
import com.tonyodev.fetch2core.server.FileRequest
import com.tonyodev.fetch2core.server.FileResourceTransporterWriter
import org.json.JSONObject
import java.net.ServerSocket
import java.net.Socket
import java.util.*


class FetchFileServerImpl(context: Context,
                          private val serverSocket: ServerSocket,
                          private val clearFileResourcesDatabaseOnShutdown: Boolean,
                          private val logger: FetchLogger,
                          databaseName: String,
                          private val fetchFileServerAuthenticator: FetchFileServerAuthenticator?,
                          private val fetchFileServerDelegate: FetchFileServerDelegate?,
                          private val fetchTransferListener: FetchTransferListener?,
                          private val progressReportingInMillis: Long,
                          private val persistentTimeoutInMillis: Long) : FetchFileServer {

    private val lock = Any()
    private val uuid = UUID.randomUUID().toString()
    override val id: String
        get() {
            return uuid
        }
    private val fileResourceProviderMap = Collections.synchronizedMap(mutableMapOf<String, FileResourceProvider>())
    @Volatile
    private var isTerminated = false
    private var isForcedTerminated = false
    private var isStarted = false
    private val fileResourceServerDatabase = FetchFileResourceInfoDatabase(context.applicationContext, databaseName)
    private val ioHandler = {
        val handlerThread = HandlerThread("FetchFileServer - $id")
        handlerThread.start()
        Handler(handlerThread.looper)
    }()
    private val mainHandler = Handler(Looper.getMainLooper())

    override val port: Int
        get() {
            return if (!isTerminated) {
                serverSocket.localPort
            } else {
                0
            }
        }

    override val address: String
        get() {
            return if (!isTerminated) {
                serverSocket.inetAddress.hostAddress
            } else {
                "00:00:00:00"
            }
        }

    override val isShutDown: Boolean
        get() {
            return isTerminated
        }

    override fun start() {
        synchronized(lock) {
            throwIfTerminated()
            if (!isStarted && !isTerminated) {
                isStarted = true
                Thread {
                    while (!isTerminated) {
                        try {
                            val client = serverSocket.accept()
                            if (!isTerminated) {
                                processClient(client)
                            } else {
                                client.close()
                            }
                        } catch (e: Exception) {
                            logger.e(TAG + "- ${e.message}")
                        }
                    }
                    cleanUpServer()
                }.start()
            }
        }
    }

    private fun processClient(clientSocket: Socket) {
        if (!isTerminated) {
            val fileResourceProvider = FetchFileResourceProvider(
                    client = clientSocket,
                    fileResourceProviderDelegate = fileResourceProviderDelegate,
                    logger = logger,
                    ioHandler = ioHandler,
                    progressReportingInMillis = progressReportingInMillis,
                    persistentTimeoutInMillis = persistentTimeoutInMillis)
            try {
                fileResourceProviderMap[fileResourceProvider.id] = fileResourceProvider
                fileResourceProvider.execute()
            } catch (e: Exception) {
                logger.e(TAG + "- ${e.message}")
                fileResourceProviderDelegate.onFinished(fileResourceProvider.id)
            }
        }
    }

    private val fileResourceProviderDelegate = object : FileResourceProviderDelegate {

        override fun onFinished(providerId: String) {
            try {
                fileResourceProviderMap.remove(providerId)
            } catch (e: Exception) {
                logger.e(TAG + "- ${e.message}")
            }
            if (fileResourceProviderMap.isEmpty() && isTerminated) {
                cleanUpServer()
            }
        }

        override fun getFileResource(fileResourceIdentifier: String): FileResource? {
            return try {
                val id: Long = fileResourceIdentifier.toLong()
                if (id == FileRequest.CATALOG_ID) {
                    getCatalogResourceFile()
                } else {
                    fileResourceServerDatabase.get(id)?.toFileResource()
                }
            } catch (e: Exception) {
                if (fileResourceIdentifier == FileRequest.CATALOG_NAME) {
                    getCatalogResourceFile()
                } else {
                    fileResourceServerDatabase.get(fileResourceIdentifier)?.toFileResource()
                }
            }
        }

        private fun getCatalogResourceFile(): FileResource {
            val catalog = fileResourceServerDatabase.getRequestedCatalog()
            val catalogFileResourceInfo = FileResourceInfo()
            catalogFileResourceInfo.id = FileRequest.CATALOG_ID
            val catalogMap = mutableMapOf<String, String>()
            catalogMap["data"] = catalog
            catalogFileResourceInfo.extras = JSONObject(catalogMap).toString()
            catalogFileResourceInfo.name = FileRequest.CATALOG_NAME
            catalogFileResourceInfo.file = FileRequest.CATALOG_FILE
            return catalogFileResourceInfo.toFileResource()
        }

        override fun acceptAuthorization(sessionId: String, authorization: String, fileRequest: FileRequest): Boolean {
            return fetchFileServerAuthenticator?.accept(sessionId, authorization, fileRequest)
                    ?: true
        }

        override fun onClientConnected(sessionId: String, fileRequest: FileRequest) {
            mainHandler.post {
                fetchFileServerDelegate?.onClientConnected(sessionId, fileRequest)
            }
        }

        override fun onClientDidProvideExtras(sessionId: String, extras: Extras, fileRequest: FileRequest) {
            mainHandler.post {
                fetchFileServerDelegate?.onClientDidProvideExtras(sessionId, extras, fileRequest)
            }
        }

        override fun onClientDisconnected(sessionId: String, fileRequest: FileRequest) {
            mainHandler.post {
                fetchFileServerDelegate?.onClientDisconnected(sessionId, fileRequest)
            }
        }

        override fun getCatalog(page: Int, size: Int): String {
            return fileResourceServerDatabase.getRequestedCatalog(page, size)
        }

        override fun getFileInputResourceWrapper(sessionId: String, fileRequest: FileRequest, fileResource: FileResource, fileOffset: Long): InputResourceWrapper? {
            return fetchFileServerDelegate?.getFileInputResourceWrapper(sessionId, fileRequest, fileResource, fileOffset)
        }

        override fun onStarted(sessionId: String, fileRequest: FileRequest, fileResource: FileResource) {
            mainHandler.post {
                fetchTransferListener?.onStarted(sessionId, fileRequest, fileResource)
            }
        }

        override fun onProgress(sessionId: String, fileRequest: FileRequest, fileResource: FileResource, progress: Int) {
            mainHandler.post {
                fetchTransferListener?.onProgress(sessionId, fileRequest, fileResource, progress)
            }
        }

        override fun onComplete(sessionId: String, fileRequest: FileRequest, fileResource: FileResource) {
            mainHandler.post {
                fetchTransferListener?.onComplete(sessionId, fileRequest, fileResource)
            }
        }

        override fun onError(sessionId: String, fileRequest: FileRequest, fileResource: FileResource, throwable: Throwable) {
            mainHandler.post {
                fetchTransferListener?.onError(sessionId, fileRequest, fileResource, throwable)
            }
        }

        override fun onCustomRequest(sessionId: String, fileRequest: FileRequest,
                                     fileResourceTransporterWriter: FileResourceTransporterWriter,
                                     interruptMonitor: InterruptMonitor) {
            fetchFileServerDelegate?.onCustomRequest(sessionId, fileRequest,
                    fileResourceTransporterWriter, interruptMonitor)
        }

    }

    private fun cleanUpServer() {
        try {
            if (!serverSocket.isClosed) {
                serverSocket.close()
            }
        } catch (e: Exception) {
            logger.e(TAG + "- ${e.message}")
        }
        try {
            fileResourceProviderMap.clear()
        } catch (e: Exception) {
            logger.e(TAG + "- ${e.message}")
        }
        try {
            if (clearFileResourcesDatabaseOnShutdown) {
                ioHandler.post {
                    fileResourceServerDatabase.deleteAll()
                    try {
                        fileResourceServerDatabase.close()
                    } catch (e: Exception) {
                        logger.e(TAG + "- ${e.message}")
                    }
                    try {
                        ioHandler.removeCallbacksAndMessages(null)
                        ioHandler.looper.quit()
                    } catch (e: Exception) {
                        logger.e(TAG + "- ${e.message}")
                    }
                }
            } else {
                try {
                    fileResourceServerDatabase.close()
                } catch (e: Exception) {
                    logger.e(TAG + "- ${e.message}")
                }
                try {
                    ioHandler.removeCallbacksAndMessages(null)
                    ioHandler.looper.quit()
                } catch (e: Exception) {
                    logger.e(TAG + "- ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.e(TAG + "- ${e.message}")
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
            val iterator = fileResourceProviderMap.values.iterator()
            while (iterator.hasNext()) {
                iterator.next().interrupt()
            }
            if (fileResourceProviderMap.isEmpty()) {
                cleanUpServer()
            }
        } catch (e: Exception) {
            logger.e(TAG + "- ${e.message}")
        }
    }

    override fun addFileResource(fileResource: FileResource) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                throwIfAddingReservedCatalogInfo(fileResource)
                if (fileResource.md5.isEmpty()) {
                    fileResource.md5 = getMd5CheckSumForFileResource(fileResource)
                }
                fileResourceServerDatabase.insert(fileResource.toFileResourceInfo())
            }
        }
    }

    override fun addFileResources(fileResources: Collection<FileResource>) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                fileResources.forEach {
                    throwIfAddingReservedCatalogInfo(it)
                    if (it.md5.isEmpty()) {
                        it.md5 = getMd5CheckSumForFileResource(it)
                    }
                }
                fileResourceServerDatabase.insert(fileResources.map { it.toFileResourceInfo() })
            }
        }
    }

    override fun removeFileResource(fileResource: FileResource) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                fileResourceServerDatabase.delete(fileResource.toFileResourceInfo())
                val iterator = fileResourceProviderMap.iterator()
                while (iterator.hasNext()) {
                    val provider = iterator.next().value
                    if (provider.isServingFileResource(fileResource)) {
                        provider.interrupt()
                        break
                    }
                }
            }
        }
    }

    override fun removeAllFileResources() {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                fileResourceServerDatabase.deleteAll()
                interruptAllProviders()
            }
        }
    }

    override fun removeFileResources(fileResources: Collection<FileResource>) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                fileResourceServerDatabase.delete(fileResources.map { it.toFileResourceInfo() })
                fileResources.forEach {
                    val iterator = fileResourceProviderMap.iterator()
                    while (iterator.hasNext()) {
                        val provider = iterator.next().value
                        if (provider.isServingFileResource(it)) {
                            provider.interrupt()
                            break
                        }
                    }
                }
            }
        }
    }

    override fun getFileResources(func: Func<List<FileResource>>) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                val filesResources = fileResourceServerDatabase.get()
                mainHandler.post {
                    func.call(filesResources.map { it.toFileResource() })
                }
            }
        }
    }

    override fun containsFileResource(fileResourceId: Long, func: Func<Boolean>) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                val fileResource = fileResourceServerDatabase.get(fileResourceId)
                mainHandler.post {
                    if (fileResource != null) {
                        func.call(true)
                    } else {
                        func.call(false)
                    }
                }
            }
        }
    }

    override fun getFileResource(fileResourceId: Long, func2: Func2<FileResource?>) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                val fileResource = fileResourceServerDatabase.get(fileResourceId)
                mainHandler.post {
                    func2.call(fileResource?.toFileResource())
                }
            }
        }
    }

    override fun getCatalog(func: Func<String>) {
        synchronized(lock) {
            throwIfTerminated()
            ioHandler.post {
                val catalog = fileResourceServerDatabase.getRequestedCatalog()
                mainHandler.post {
                    func.call(catalog)
                }
            }
        }
    }

    private fun getMd5CheckSumForFileResource(fileResource: FileResource): String {
        return getFileMd5String(fileResource.file) ?: ""
    }

    private fun throwIfTerminated() {
        if (isTerminated) {
            throw Exception("FetchFileServer was already Shutdown. It cannot be restarted. Get a new Instance.")
        }
    }

    private fun throwIfAddingReservedCatalogInfo(fileResource: FileResource) {
        if (fileResource.id == FileRequest.CATALOG_ID
                || fileResource.name == FileRequest.CATALOG_NAME
                || fileResource.file == FileRequest.CATALOG_FILE) {
            throw IllegalArgumentException("File Resources 'id' cannot be: ${FileRequest.CATALOG_ID} " +
                    "and 'name' cannot be: Catalog.json and " +
                    "'file' cannot be: /Catalog.json")
        }
    }

    companion object {
        const val TAG = "FetchFileServer"
    }

}