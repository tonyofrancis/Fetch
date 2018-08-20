package com.tonyodev.fetch2fileserver.provider

import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.FileResource
import com.tonyodev.fetch2core.InputResourceWrapper
import com.tonyodev.fetch2core.server.FileRequest
import com.tonyodev.fetch2core.InterruptMonitor
import com.tonyodev.fetch2fileserver.database.FileResourceInfo
import com.tonyodev.fetch2core.server.FileResourceTransporterWriter
import java.util.*

interface FileResourceProviderDelegate {

    fun getFileResource(fileResourceIdentifier: String): FileResource?

    fun onFinished(providerId: String)

    fun acceptAuthorization(sessionId: String, authorization: String, fileRequest: FileRequest): Boolean

    fun onClientDidProvideExtras(sessionId: String, extras: Extras, fileRequest: FileRequest)

    fun onClientConnected(sessionId: String, fileRequest: FileRequest)

    fun onClientDisconnected(sessionId: String, fileRequest: FileRequest)

    fun onStarted(sessionId: String, fileRequest: FileRequest, fileResource: FileResource)

    fun onProgress(sessionId: String, fileRequest: FileRequest, fileResource: FileResource, progress: Int)

    fun onComplete(sessionId: String, fileRequest: FileRequest, fileResource: FileResource)

    fun onError(sessionId: String, fileRequest: FileRequest, fileResource: FileResource, throwable: Throwable)

    fun getCatalog(page: Int, size: Int): String

    fun getFileInputResourceWrapper(sessionId: String, fileRequest: FileRequest, fileResource: FileResource, fileOffset: Long): InputResourceWrapper?

    fun onCustomRequest(sessionId: String, fileRequest: FileRequest, fileResourceTransporterWriter: FileResourceTransporterWriter, interruptMonitor: InterruptMonitor)

}