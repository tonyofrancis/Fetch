package com.tonyodev.fetch2fileserver

import android.content.Context
import java.net.ServerSocket

interface FetchFileServer {

    val id: String

    val port: Int

    val address: String

    val isShutDown: Boolean

    fun start()

    fun shutDown(forced: Boolean = false)

    fun setAuthenticator(fetchFileServerAuthenticator: FetchFileServerAuthenticator?)

    fun setDelegate(fetchFileServerDelegate: FetchFileServerDelegate?)

    fun setTransferProgressListener(fetchTransferProgressListener: FetchTransferProgressListener?)

    fun addContentFile(contentFile: ContentFile)

    fun addContentFiles(contentFiles: Collection<ContentFile>)

    fun removeContentFile(contentFile: ContentFile)

    fun removeContentFiles(contentFiles: Collection<ContentFile>)

    fun removeAllContentFiles()

    fun containsContentFile(contentId: Int, callback: (Boolean) -> Unit)

    fun getContentFiles(callback: (List<ContentFile>) -> Unit)

    fun getFullCatalog(callback: (String) -> Unit)

    fun getContentFile(contentId: Int, callback: (ContentFile?) -> Unit)

    class Builder(private val context: Context) {

        private var serverSocket = ServerSocket(0)
        private var clearDatabaseOnShutdown = false
        private var logger = FetchFileServerLogger()
        private var fileServerAuthenticator: FetchFileServerAuthenticator? = null
        private var fileServerDelegate: FetchFileServerDelegate? = null
        private var transferProgressListener: FetchTransferProgressListener? = null
        private var contentFileDatabaseName = "LibFetchFileServerDatabaseLib.db"

        fun setServerSocket(serverSocket: ServerSocket): Builder {
            this.serverSocket = serverSocket
            return this
        }

        fun setClearDatabaseOnShutdown(clear: Boolean): Builder {
            this.clearDatabaseOnShutdown = clear
            return this
        }

        fun setLogger(logger: FetchFileServerLogger): Builder {
            this.logger = logger
            return this
        }

        fun setAuthenticator(fetchFileServerAuthenticator: FetchFileServerAuthenticator): Builder {
            this.fileServerAuthenticator = fetchFileServerAuthenticator
            return this
        }

        fun setDelegate(fetchFileServerDelegate: FetchFileServerDelegate): Builder {
            this.fileServerDelegate = fetchFileServerDelegate
            return this
        }

        fun setTransferProgressListener(fetchTransferProgressListener: FetchTransferProgressListener): Builder {
            this.transferProgressListener = fetchTransferProgressListener
            return this
        }

        fun setFileServerDatabaseName(databaseName: String): Builder {
            if (databaseName.isNotEmpty()) {
                this.contentFileDatabaseName = databaseName
            }
            return this
        }

        fun build(): FetchFileServer {
            val fetchContentFileServer = FetchFileServerImpl(context = context.applicationContext,
                    serverSocket = serverSocket,
                    clearContentFileDatabaseOnShutdown = clearDatabaseOnShutdown,
                    logger = logger,
                    databaseName = contentFileDatabaseName)
            fetchContentFileServer.setAuthenticator(fileServerAuthenticator)
            fetchContentFileServer.setDelegate(fileServerDelegate)
            fetchContentFileServer.setTransferProgressListener(transferProgressListener)
            return fetchContentFileServer
        }

    }

}