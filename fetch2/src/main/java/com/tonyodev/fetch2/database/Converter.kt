package com.tonyodev.fetch2.database

import android.arch.persistence.room.TypeConverter
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.util.EMPTY_JSON_OBJECT_STRING
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.Extras
import org.json.JSONObject

class Converter {

    @TypeConverter
    fun fromStatusValue(value: Int): Status {
        return Status.valueOf(value)
    }

    @TypeConverter
    fun toStatusValue(status: Status): Int {
        return status.value
    }

    @TypeConverter
    fun fromJsonString(jsonString: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val json = JSONObject(jsonString)
        json.keys().forEach {
            map[it] = json.getString(it)
        }
        return map
    }

    @TypeConverter
    fun toHeaderStringsMap(headerMap: Map<String, String>): String {
        return if (headerMap.isEmpty()) {
            EMPTY_JSON_OBJECT_STRING
        } else {
            val json = JSONObject()
            headerMap.iterator().forEach {
                json.put(it.key, it.value)
            }
            json.toString()
        }
    }

    @TypeConverter
    fun fromPriorityValue(value: Int): Priority {
        return Priority.valueOf(value)
    }

    @TypeConverter
    fun toPriorityValue(priority: Priority): Int {
        return priority.value
    }

    @TypeConverter
    fun fromErrorValue(value: Int): Error {
        return Error.valueOf(value)
    }

    @TypeConverter
    fun toErrorValue(error: Error): Int {
        return error.value
    }

    @TypeConverter
    fun fromNetworkTypeValue(value: Int): NetworkType {
        return NetworkType.valueOf(value)
    }

    @TypeConverter
    fun toNetworkTypeValue(networkType: NetworkType): Int {
        return networkType.value
    }

    @TypeConverter
    fun toEnqueueActionValue(enqueueAction: EnqueueAction): Int {
        return enqueueAction.value
    }

    @TypeConverter
    fun fromEnqueueActionValue(value: Int): EnqueueAction {
        return EnqueueAction.valueOf(value)
    }

    @TypeConverter
    fun fromExtrasToString(extras: Extras): String {
        return if (extras.isEmpty()) {
            EMPTY_JSON_OBJECT_STRING
        } else {
            val json = JSONObject()
            val map = extras.map
            map.iterator().forEach {
                json.put(it.key, it.value)
            }
            json.toString()
        }
    }

    @TypeConverter
    fun fromExtrasJsonToExtras(jsonString: String): Extras {
        val map = mutableMapOf<String, String>()
        val json = JSONObject(jsonString)
        json.keys().forEach {
            map[it] = json.getString(it)
        }
        return Extras(map)
    }

}
