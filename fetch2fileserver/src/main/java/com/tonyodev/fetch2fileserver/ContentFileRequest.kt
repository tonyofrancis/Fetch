package com.tonyodev.fetch2fileserver

import java.lang.StringBuilder

data class ContentFileRequest(val type: Int = TYPE_INVALID,
                              val contentFileId: String = CATALOG_ID.toString(),
                              val rangeStart: Long = 0L,
                              val rangeEnd: Long = -1L,
                              val authorization: String = "",
                              val client: String = "",
                              val customData: String = "",
                              val page: Int = 0,
                              val size: Int = 0,
                              val persistConnection: Boolean = true) {

    val toJsonString: String
        get() {
            val builder = StringBuilder()
                    .append('{')
                    .append("\"Type\":").append(type).append(',')
                    .append("\"ContentFileId\":").append("\"$contentFileId\"").append(',')
                    .append("\"RangeStart\":").append(rangeStart).append(',')
                    .append("\"RangeEnd\":").append(rangeEnd).append(',')
                    .append("\"Authorization\":").append("\"$authorization\"").append(',')
                    .append("\"Client\":").append("\"$client\"").append(',')
                    .append("\"CustomData\":").append("\"$customData\"").append(',')
                    .append("\"Page\":").append(page).append(',')
                    .append("\"Size\":").append(size).append(',')
                    .append("\"PersistConnection\":").append(persistConnection)
                    .append('}')
            return builder.toString()
        }

    companion object {

        const val TYPE_INVALID = -1
        const val TYPE_PING = 0
        const val TYPE_FILE = 1
        const val TYPE_CATALOG = 2
        const val CATALOG_ID = -1L
        const val FIELD_TYPE = "Type"
        const val FIELD_CONTENT_FILE_ID = "ContentFileId"
        const val FIELD_RANGE_START = "RangeStart"
        const val FIELD_RANGE_END = "RangeEnd"
        const val FIELD_AUTHORIZATION = "Authorization"
        const val FIELD_CLIENT = "Client"
        const val FIELD_CUSTOM_DATA = "CustomData"
        const val FIELD_PAGE = "Page"
        const val FIELD_SIZE = "Size"
        const val FIELD_PERSIST_CONNECTION = "PersistConnection"

    }

}