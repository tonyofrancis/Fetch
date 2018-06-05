package com.tonyodev.fetch2fileserver.provider

import com.tonyodev.fetch2fileserver.ContentFile
import com.tonyodev.fetch2fileserver.ContentFileRequest
import com.tonyodev.fetch2.util.InterruptMonitor
import com.tonyodev.fetch2fileserver.transporter.ContentFileTransporterWriter
import java.io.InputStream
import java.util.*

interface ContentFileProviderDelegate {

    fun getContentFile(contentFileIdentifier: String): ContentFile?
    fun onFinished(id: UUID)
    fun acceptAuthorization(authorization: String, contentFileRequest: ContentFileRequest): Boolean
    fun onClientDidProvideCustomData(client: String, customData: String, contentFileRequest: ContentFileRequest)
    fun onClientConnected(client: String, contentFileRequest: ContentFileRequest)
    fun onClientDisconnected(client: String)
    fun onProgress(client: String, contentFile: ContentFile, progress: Int)
    fun getCatalog(page: Int, size: Int): String
    fun getFileInputStream(contentFile: ContentFile, fileOffset: Long): InputStream?
    fun onCustomRequest(client: String, contentFileRequest: ContentFileRequest,
                        contentFileTransporterWriter: ContentFileTransporterWriter, interruptMonitor: InterruptMonitor)
}