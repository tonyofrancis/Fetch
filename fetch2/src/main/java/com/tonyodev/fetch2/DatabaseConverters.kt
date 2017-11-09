package com.tonyodev.fetch2

import android.arch.persistence.room.TypeConverter
import android.support.v4.util.ArrayMap

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by tonyofrancis on 6/14/17.
 */

class DatabaseConverters {

    @TypeConverter
    fun headerStringToList(headers: String): Map<String, String> {

        val headerMap = ArrayMap<String, String>()

        try {

            val jsonObject = JSONObject(headers)
            val keys = jsonObject.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                headerMap.put(key, jsonObject.getString(key))
            }

        } catch (e: JSONException) {
        }

        return headerMap
    }

    @TypeConverter
    fun headerListToString(headerMap: Map<String, String>): String {

        var headerString: String

        try {

            val headerObject = JSONObject()

            for (key in headerMap.keys) {
                headerObject.put(key, headerMap[key])
            }

            headerString = headerObject.toString()
        } catch (e: JSONException) {
            headerString = "{}"
        }

        return headerString
    }

}
