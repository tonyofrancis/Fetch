package com.tonyodev.fetch2fileserver.provider

import com.tonyodev.fetch2fileserver.ContentFile
import com.tonyodev.fetch2fileserver.FileRequest
import com.tonyodev.fetch2.util.InterruptMonitor
import com.tonyodev.fetch2fileserver.transporter.ContentFileTransporterWriter
import java.io.InputStream
import java.util.*

interface ContentFileProviderDelegate {

    fun getContentFile(contentFileIdentifier: String): ContentFile?

    fun onFinished(id: UUID)

    fun acceptAuthorization(authorization: String, fileRequest: FileRequest): Boolean

    fun onClientDidProvideCustomData(client: String, customData: String, fileRequest: FileRequest)

    fun onClientConnected(client: String, fileRequest: FileRequest)

    fun onClientDisconnected(client: String)

    fun onProgress(client: String, contentFile: ContentFile, progress: Int)

    fun getCatalog(page: Int, size: Int): String

    fun getFileInputStream(contentFile: ContentFile, fileOffset: Long): InputStream?

    fun onCustomRequest(client: String, fileRequest: FileRequest,
                        contentFileTransporterWriter: ContentFileTransporterWriter, interruptMonitor: InterruptMonitor)

}