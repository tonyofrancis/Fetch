package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2.util.InterruptMonitor
import com.tonyodev.fetch2fileserver.transporter.ContentFileTransporterWriter
import java.io.InputStream

interface FetchFileServerDelegate {

    fun onClientConnected(client: String, fileRequest: FileRequest)

    fun onClientDidProvideCustomData(client: String, customData: String, fileRequest: FileRequest)

    fun onClientDisconnected(client: String)

    fun getFileInputStream(contentFile: ContentFile, fileOffset: Long): InputStream?

    fun onCustomRequest(client: String, fileRequest: FileRequest,
                        contentFileTransporterWriter: ContentFileTransporterWriter, interruptMonitor: InterruptMonitor)

}