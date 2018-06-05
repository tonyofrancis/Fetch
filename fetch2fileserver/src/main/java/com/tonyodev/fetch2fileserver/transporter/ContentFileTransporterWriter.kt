package com.tonyodev.fetch2fileserver.transporter

import com.tonyodev.fetch2fileserver.ContentFileRequest
import com.tonyodev.fetch2fileserver.ContentFileResponse

interface ContentFileTransporterWriter {

    fun sendContentFileRequest(contentFileRequest: ContentFileRequest)
    fun sendContentFileResponse(contentFileResponse: ContentFileResponse)
    fun sendRawBytes(byteArray: ByteArray, offset: Int, length: Int)

}