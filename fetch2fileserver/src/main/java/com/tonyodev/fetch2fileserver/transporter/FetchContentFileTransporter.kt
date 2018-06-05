package com.tonyodev.fetch2fileserver.transporter

import com.tonyodev.fetch2fileserver.ContentFileRequest
import com.tonyodev.fetch2fileserver.ContentFileResponse
import com.tonyodev.fetch2fileserver.ContentFileResponse.Companion.FIELD_CONNECTION
import com.tonyodev.fetch2fileserver.ContentFileResponse.Companion.FIELD_CONTENT_LENGTH
import com.tonyodev.fetch2fileserver.ContentFileResponse.Companion.FIELD_DATE
import com.tonyodev.fetch2fileserver.ContentFileResponse.Companion.FIELD_MD5
import com.tonyodev.fetch2fileserver.ContentFileResponse.Companion.FIELD_STATUS
import com.tonyodev.fetch2fileserver.ContentFileResponse.Companion.FIELD_TYPE
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketAddress

class FetchContentFileTransporter(private val client: Socket = Socket()) : ContentFileTransporter {


    private lateinit var dataInput: DataInputStream
    private lateinit var dataOutput: DataOutputStream
    private val lock = Any()
    @Volatile
    private var closed = false

    override val isClosed: Boolean
        get() {
            synchronized(lock) {
                return closed
            }
        }

    init {
        if (client.isConnected && !client.isClosed) {
            dataInput = DataInputStream(client.getInputStream())
            dataOutput = DataOutputStream(client.getOutputStream())
        }
        if (client.isClosed) {
            closed = true
        }
    }

    override fun connect(socketAddress: SocketAddress) {
        synchronized(lock) {
            throwExceptionIfClosed()
            client.connect(socketAddress)
            dataInput = DataInputStream(client.getInputStream())
            dataOutput = DataOutputStream(client.getOutputStream())
        }
    }

    override fun receiveContentFileRequest(): ContentFileRequest? {
        return synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            val json = JSONObject(dataInput.readUTF())
            val requestType = json.getInt(ContentFileRequest.FIELD_TYPE)
            val contentFileId = json.getString(ContentFileRequest.FIELD_CONTENT_FILE_ID)
            var rangeStart = json.getLong(ContentFileRequest.FIELD_RANGE_START)
            var rangeEnd = json.getLong(ContentFileRequest.FIELD_RANGE_END)
            val authorization = json.getString(ContentFileRequest.FIELD_AUTHORIZATION)
            val client = json.getString(ContentFileRequest.FIELD_CLIENT)
            val customData = json.getString(ContentFileRequest.FIELD_CUSTOM_DATA)
            var page = json.getInt(ContentFileRequest.FIELD_PAGE)
            var size = json.getInt(ContentFileRequest.FIELD_SIZE)
            if ((rangeStart < 0L || rangeStart > rangeEnd) && rangeEnd > -1) {
                rangeStart = 0L
            }
            if (rangeEnd < 0L || rangeEnd < rangeStart) {
                rangeEnd = -1L
            }
            if (page < -1) {
                page = -1
            }
            if (size < -1) {
                size = -1
            }
            val persistConnection = json.getBoolean(ContentFileRequest.FIELD_PERSIST_CONNECTION)
            ContentFileRequest(
                    type = requestType,
                    contentFileId = contentFileId,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                    authorization = authorization,
                    client = client,
                    customData = customData,
                    page = page,
                    size = size,
                    persistConnection = persistConnection)
        }
    }

    override fun sendContentFileRequest(contentFileRequest: ContentFileRequest) {
        synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            dataOutput.writeUTF(contentFileRequest.toJsonString)
            dataOutput.flush()
        }
    }

    override fun receiveContentFileResponse(): ContentFileResponse? {
        return synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            val json = JSONObject(dataInput.readUTF())
            val status = json.getInt(FIELD_STATUS)
            val requestType = json.getInt(FIELD_TYPE)
            val connection = json.getInt(FIELD_CONNECTION)
            val date = json.getLong(FIELD_DATE)
            val contentLength = json.getLong(FIELD_CONTENT_LENGTH)
            val md5 = json.getString(FIELD_MD5)
            ContentFileResponse(
                    status = status,
                    type = requestType,
                    connection = connection,
                    date = date,
                    contentLength = contentLength,
                    md5 = md5)
        }
    }

    override fun sendContentFileResponse(contentFileResponse: ContentFileResponse) {
        synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            dataOutput.writeUTF(contentFileResponse.toJsonString)
            dataOutput.flush()
        }
    }

    override fun sendRawBytes(byteArray: ByteArray, offset: Int, length: Int) {
        synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            dataOutput.write(byteArray, offset, length)
            dataOutput.flush()
        }
    }

    override fun readRawBytes(byteArray: ByteArray, offset: Int, length: Int): Int {
        return synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            dataInput.read(byteArray, offset, length)
        }
    }

    override fun getInputStream(): InputStream {
        synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            return dataInput
        }
    }

    override fun getOutputStream(): OutputStream {
        synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            return dataOutput
        }
    }

    override fun close() {
        synchronized(lock) {
            if (!closed) {
                closed = true
                try {
                    dataInput.close()
                } catch (e: Exception) {
                }
                try {
                    dataOutput.close()
                } catch (e: Exception) {
                }
                try {
                    client.close()
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw Exception("ClientContentFileTransporter is already closed.")
        }
    }

    @Suppress("SENSELESS_COMPARISON")
    private fun throwIfNotConnected() {
        if (dataInput == null || dataOutput == null) {
            throw Exception("You forgot to call connect before calling this method.")
        }
    }


}