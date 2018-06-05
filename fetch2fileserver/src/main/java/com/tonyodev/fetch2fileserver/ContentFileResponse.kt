package com.tonyodev.fetch2fileserver

import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.util.*

data class ContentFileResponse(val status: Int = HttpURLConnection.HTTP_UNSUPPORTED_TYPE,
                               val type: Int = ContentFileRequest.TYPE_INVALID,
                               val connection: Int = CLOSE_CONNECTION,
                               val date: Long = Date().time,
                               val contentLength: Long = 0,
                               val md5: String = "") {

    val toJsonString: String
        get() {
            val builder = StringBuilder()
                    .append('{')
                    .append("\"Status\":").append(status).append(',')
                    .append("\"Md5\":").append("\"$md5\"").append(',')
                    .append("\"Connection\":").append(connection).append(',')
                    .append("\"Date\":").append(date).append(',')
                    .append("\"ContentLength\":").append(contentLength).append(',')
                    .append("\"Type\":").append(type)
                    .append('}')
            return builder.toString()
        }

    companion object {
        const val CLOSE_CONNECTION = 0
        const val OPEN_CONNECTION = 1
        const val FIELD_STATUS = "Status"
        const val FIELD_TYPE = "Type"
        const val FIELD_CONNECTION = "Connection"
        const val FIELD_DATE = "Date"
        const val FIELD_CONTENT_LENGTH = "ContentLength"
        const val FIELD_MD5 = "Md5"
    }

}