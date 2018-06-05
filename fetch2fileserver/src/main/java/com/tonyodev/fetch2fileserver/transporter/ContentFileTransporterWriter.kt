package com.tonyodev.fetch2fileserver.transporter

import com.tonyodev.fetch2fileserver.FileRequest
import com.tonyodev.fetch2fileserver.FileResponse

interface ContentFileTransporterWriter {

    fun sendContentFileRequest(fileRequest: FileRequest)

    fun sendContentFileResponse(fileResponse: FileResponse)

    fun sendRawBytes(byteArray: ByteArray, offset: Int, length: Int)

}