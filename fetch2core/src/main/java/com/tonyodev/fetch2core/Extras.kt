package com.tonyodev.fetch2core

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject

open class Extras(protected val data: Map<String, String>) : Parcelable {

    fun getString(key: String, defaultValue: String): String {
        return data[key] ?: defaultValue
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return data[key]?.toInt() ?: defaultValue
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return data[key]?.toLong() ?: defaultValue
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return data[key]?.toBoolean() ?: defaultValue
    }

    fun getDouble(key: String, defaultValue: Double): Double {
        return data[key]?.toDouble() ?: defaultValue
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return data[key]?.toFloat() ?: defaultValue
    }

    fun toMutableExtras(): MutableExtras {
        return MutableExtras(data.toMutableMap())
    }

    fun toJSONString(): String {
        return if (isEmpty()) {
            "{}"
        } else {
            JSONObject(map).toString()
        }
    }

    fun toJSONObject(): JSONObject {
        return if (isEmpty()) {
            JSONObject()
        } else {
            JSONObject(map)
        }
    }

    open fun copy(): Extras {
        return Extras(data.toMap())
    }

    fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return data.isNotEmpty()
    }

    val size: Int
        get() {
            return data.size
        }

    val map: Map<String, String>
        get() {
            return data.toMap()
        }

    override fun toString(): String {
        return "Extras(data=$data)"
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(HashMap(data))
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Extras
        if (data != other.data) return false
        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    companion object CREATOR : Parcelable.Creator<Extras> {

        @Suppress("UNCHECKED_CAST")
        override fun createFromParcel(source: Parcel): Extras {
            return Extras(source.readSerializable() as HashMap<String, String>)
        }

        override fun newArray(size: Int): Array<Extras?> {
            return arrayOfNulls(size)
        }

        @JvmStatic
        val emptyExtras = Extras(emptyMap())

    }


}