package com.tonyodev.fetch2core.server

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.util.*

/**
 * Response object sent to the client as JSON.
 * **/
data class FileResponse(val status: Int = HttpURLConnection.HTTP_UNSUPPORTED_TYPE,
                        val type: Int = FileRequest.TYPE_INVALID,
                        val connection: Int = CLOSE_CONNECTION,
                        val date: Long = Date().time,
                        val contentLength: Long = 0,
                        val md5: String = "",
                        val sessionId: String = "") : Parcelable, Serializable {

    val toJsonString: String
        get() {
            val builder = StringBuilder()
                    .append('{')
                    .append("\"Status\":").append(status).append(',')
                    .append("\"Md5\":").append("\"$md5\"").append(',')
                    .append("\"Connection\":").append(connection).append(',')
                    .append("\"Date\":").append(date).append(',')
                    .append("\"Content-Length\":").append(contentLength).append(',')
                    .append("\"Type\":").append(type).append(',')
                    .append("\"SessionId\":").append(sessionId)
                    .append('}')
            return builder.toString()
        }


    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(status)
        dest.writeInt(type)
        dest.writeInt(connection)
        dest.writeLong(date)
        dest.writeLong(contentLength)
        dest.writeString(md5)
        dest.writeString(sessionId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FileResponse> {

        const val CLOSE_CONNECTION = 0
        const val OPEN_CONNECTION = 1
        const val FIELD_STATUS = "status"
        const val FIELD_TYPE = "type"
        const val FIELD_CONNECTION = "connection"
        const val FIELD_DATE = "date"
        const val FIELD_CONTENT_LENGTH = "content-length"
        const val FIELD_MD5 = "md5"
        const val FIELD_SESSION_ID = "sessionid"


        override fun createFromParcel(source: Parcel): FileResponse {
            return FileResponse(
                    status = source.readInt(),
                    type = source.readInt(),
                    connection = source.readInt(),
                    date = source.readLong(),
                    contentLength = source.readLong(),
                    md5 = source.readString() ?: "",
                    sessionId = source.readString() ?: "")
        }

        override fun newArray(size: Int): Array<FileResponse?> {
            return arrayOfNulls(size)
        }

    }

}