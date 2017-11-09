package com.tonyodev.fetch2

import android.support.v4.util.ArrayMap

class Request @JvmOverloads constructor(val url: String, val absoluteFilePath: String, headers: MutableMap<String, String>? = null) {

    val id: Long
    private val headers: MutableMap<String, String>
    internal var groupId: String

    init {
        var realHeaders = headers
        if (url.isEmpty()) {
            throw IllegalArgumentException("Url cannot be null or empty")
        }

        if (absoluteFilePath.isEmpty()) {
            throw IllegalArgumentException("AbsoluteFilePath cannot be null or empty")
        }

        if (realHeaders == null) {
            realHeaders = ArrayMap()
        }
        this.headers = realHeaders
        this.groupId = ""
        this.id = generateId()
    }

    fun getHeaders(): Map<String, String> {
        return headers
    }

    fun putHeader(key: String, value: String?) {
        var realValue = value

        if (realValue == null) realValue = ""

        headers.put(key, realValue)
    }

    fun getGroupId(): String {
        return groupId
    }

    fun setGroupId(groupId: String) {
        this.groupId = groupId
    }

    private fun generateId(): Long {
        var code1: Long = 0
        var code2: Long = 0

        for (c in url.toCharArray()) {
            code1 = code1 * 31 + c.toLong()
        }

        for (c in absoluteFilePath.toCharArray()) {
            code2 = code2 * 31 + c.toLong()
        }

        return Math.abs(code1 + code2)
    }

    override fun toString(): String {
        return "{\"url\":\"$url\",\"absolutePath\":$absoluteFilePath\"}"
    }
}
