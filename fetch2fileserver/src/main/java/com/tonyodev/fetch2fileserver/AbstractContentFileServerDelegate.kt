package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2.util.InterruptMonitor
import com.tonyodev.fetch2fileserver.transporter.ContentFileTransporterWriter
import java.io.InputStream

abstract class AbstractContentFileServerDelegate : ContentFileServerDelegate {

    override fun onClientConnected(client: String, contentFileRequest: ContentFileRequest) {

    }

    override fun onClientDidProvideCustomData(client: String, customData: String, contentFileRequest: ContentFileRequest) {

    }

    override fun onClientDisconnected(client: String) {

    }

    override fun getFileInputStream(contentFile: ContentFile, fileOffset: Long): InputStream? {
        return null
    }

    override fun onCustomRequest(client: String, contentFileRequest: ContentFileRequest,
                                 contentFileTransporterWriter: ContentFileTransporterWriter, interruptMonitor: InterruptMonitor) {

    }

}