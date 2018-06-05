package com.tonyodev.fetch2fileserver.transporter

import com.tonyodev.fetch2fileserver.ContentFileRequest
import com.tonyodev.fetch2fileserver.ContentFileResponse
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketAddress

interface ContentFileTransporter : ContentFileTransporterWriter {

    val isClosed: Boolean

    fun connect(socketAddress: SocketAddress)
    fun receiveContentFileRequest(): ContentFileRequest?
    fun receiveContentFileResponse(): ContentFileResponse?
    fun readRawBytes(byteArray: ByteArray, offset: Int, length: Int): Int
    fun getInputStream(): InputStream
    fun getOutputStream(): OutputStream
    fun close()

    companion object {
        const val BUFFER_SIZE = 8192
    }

}