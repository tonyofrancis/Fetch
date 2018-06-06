package com.tonyodev.fetch2fileserver.transporter

import com.tonyodev.fetch2fileserver.FileRequest
import com.tonyodev.fetch2fileserver.FileResponse

interface FileResourceTransporterWriter {

    fun sendFileRequest(fileRequest: FileRequest)

    fun sendFileResponse(fileResponse: FileResponse)

    fun sendRawBytes(byteArray: ByteArray, offset: Int, length: Int)

}