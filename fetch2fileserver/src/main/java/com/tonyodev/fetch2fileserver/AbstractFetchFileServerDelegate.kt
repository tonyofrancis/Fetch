package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2.util.InterruptMonitor
import com.tonyodev.fetch2fileserver.transporter.ContentFileTransporterWriter
import java.io.InputStream

abstract class AbstractFetchFileServerDelegate : FetchFileServerDelegate {

    override fun onClientConnected(client: String, fileRequest: FileRequest) {

    }

    override fun onClientDidProvideCustomData(client: String, customData: String, fileRequest: FileRequest) {

    }

    override fun onClientDisconnected(client: String) {

    }

    override fun getFileInputStream(contentFile: ContentFile, fileOffset: Long): InputStream? {
        return null
    }

    override fun onCustomRequest(client: String, fileRequest: FileRequest,
                                 contentFileTransporterWriter: ContentFileTransporterWriter, interruptMonitor: InterruptMonitor) {

    }

}