package com.tonyodev.fetch2fileserver.provider

import com.tonyodev.fetch2fileserver.FileResource
import com.tonyodev.fetch2core.transporter.FileRequest
import com.tonyodev.fetch2core.InterruptMonitor
import com.tonyodev.fetch2fileserver.database.FileResourceInfo
import com.tonyodev.fetch2core.transporter.FileResourceTransporterWriter
import java.io.InputStream
import java.util.*

interface FileResourceProviderDelegate {

    fun getFileResource(fileResourceIdentifier: String): FileResourceInfo?

    fun onFinished(providerId: UUID)

    fun acceptAuthorization(authorization: String, fileRequest: FileRequest): Boolean

    fun onClientDidProvideCustomData(client: String, customData: String, fileRequest: FileRequest)

    fun onClientConnected(client: String, fileRequest: FileRequest)

    fun onClientDisconnected(client: String)

    fun onProgress(client: String, fileResource: FileResource, progress: Int)

    fun getCatalog(page: Int, size: Int): String

    fun getFileInputStream(fileResource: FileResource, fileOffset: Long): InputStream?

    fun onCustomRequest(client: String, fileRequest: FileRequest,
                        fileResourceTransporterWriter: FileResourceTransporterWriter, interruptMonitor: InterruptMonitor)

}