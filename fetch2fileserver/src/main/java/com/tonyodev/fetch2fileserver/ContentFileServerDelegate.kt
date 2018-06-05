package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2.util.InterruptMonitor
import com.tonyodev.fetch2fileserver.transporter.ContentFileTransporterWriter
import java.io.InputStream

interface ContentFileServerDelegate {

    fun onClientConnected(client: String, contentFileRequest: ContentFileRequest)

    fun onClientDidProvideCustomData(client: String, customData: String, contentFileRequest: ContentFileRequest)

    fun onClientDisconnected(client: String)

    fun getFileInputStream(contentFile: ContentFile, fileOffset: Long): InputStream?

    fun onCustomRequest(client: String, contentFileRequest: ContentFileRequest,
                        contentFileTransporterWriter: ContentFileTransporterWriter, interruptMonitor: InterruptMonitor)

}