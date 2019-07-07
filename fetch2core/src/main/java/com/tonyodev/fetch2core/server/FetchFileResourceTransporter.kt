package com.tonyodev.fetch2core.server

import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.server.FileResponse.CREATOR.FIELD_CONNECTION
import com.tonyodev.fetch2core.server.FileResponse.CREATOR.FIELD_CONTENT_LENGTH
import com.tonyodev.fetch2core.server.FileResponse.CREATOR.FIELD_DATE
import com.tonyodev.fetch2core.server.FileResponse.CREATOR.FIELD_MD5
import com.tonyodev.fetch2core.server.FileResponse.CREATOR.FIELD_SESSION_ID
import com.tonyodev.fetch2core.server.FileResponse.CREATOR.FIELD_STATUS
import com.tonyodev.fetch2core.server.FileResponse.CREATOR.FIELD_TYPE
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketAddress

class FetchFileResourceTransporter(private val client: Socket = Socket()) : FileResourceTransporter {


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

    override fun receiveFileRequest(): FileRequest? {
        return synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            val json = JSONObject(dataInput.readUTF())
            val requestType = json.getInt(FileRequest.FIELD_TYPE)
            val fileResourceId = json.getString(FileRequest.FIELD_FILE_RESOURCE_ID)
            var rangeStart = json.getLong(FileRequest.FIELD_RANGE_START)
            var rangeEnd = json.getLong(FileRequest.FIELD_RANGE_END)
            val authorization = json.getString(FileRequest.FIELD_AUTHORIZATION)
            val client = json.getString(FileRequest.FIELD_CLIENT)
            val extras = try {
                val map = mutableMapOf<String, String>()
                val jsonObject = JSONObject(json.getString(FileRequest.FIELD_EXTRAS))
                jsonObject.keys().forEach {
                    map[it] = jsonObject.getString(it)
                }
                Extras(map)
            } catch (e: Exception) {
                Extras.emptyExtras
            }
            var page = json.getInt(FileRequest.FIELD_PAGE)
            var size = json.getInt(FileRequest.FIELD_SIZE)
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
            val persistConnection = json.getBoolean(FileRequest.FIELD_PERSIST_CONNECTION)
            FileRequest(
                    type = requestType,
                    fileResourceId = fileResourceId,
                    rangeStart = rangeStart,
                    rangeEnd = rangeEnd,
                    authorization = authorization,
                    client = client,
                    extras = extras,
                    page = page,
                    size = size,
                    persistConnection = persistConnection)
        }
    }

    override fun sendFileRequest(fileRequest: FileRequest) {
        synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            dataOutput.writeUTF(fileRequest.toJsonString)
            dataOutput.flush()
        }
    }

    override fun receiveFileResponse(): FileResponse? {
        return synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            val json = JSONObject(dataInput.readUTF().toLowerCase())
            val status = json.getInt(FIELD_STATUS)
            val requestType = json.getInt(FIELD_TYPE)
            val connection = json.getInt(FIELD_CONNECTION)
            val date = json.getLong(FIELD_DATE)
            val contentLength = json.getLong(FIELD_CONTENT_LENGTH)
            val md5 = json.getString(FIELD_MD5)
            val sessionId = json.getString(FIELD_SESSION_ID)
            FileResponse(
                    status = status,
                    type = requestType,
                    connection = connection,
                    date = date,
                    contentLength = contentLength,
                    md5 = md5,
                    sessionId = sessionId)
        }
    }

    override fun sendFileResponse(fileResponse: FileResponse) {
        synchronized(lock) {
            throwExceptionIfClosed()
            throwIfNotConnected()
            dataOutput.writeUTF(fileResponse.toJsonString)
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
            throw Exception("FetchFileResourceTransporter is already closed.")
        }
    }

    @Suppress("SENSELESS_COMPARISON")
    private fun throwIfNotConnected() {
        if (dataInput == null || dataOutput == null) {
            throw Exception("You forgot to call connect before calling this method.")
        }
    }

}