package com.tonyodev.fetch2core

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject
import java.io.Serializable

/**
 *  Class that holds custom key/value pair data for a request and download.
 *  Use the get methods to get the custom data by type.
 *  Call the toMutableExtras() method to get a new mutable/changeable instance of
 *  this class that contains the instance data/extras.
 *
 *  Use the mutable version of this class to update information on a request/download
 *  on a fetch instance with the matching namespace for the request/download.
 *  @see com.tonyodev.fetch2.Fetch.replaceExtras(id, extras, func, func) method.
 * */
open class Extras(
        /** Map that holds the custom data.*/
        protected val data: Map<String, String>) : Parcelable, Serializable {

    /**
     * Retrieve a string value for passed in matching key.
     * @param key custom data key
     * @param defaultValue default value if value for the key does not exist.
     * @return value for the key if it exist otherwise the default value passed
     * to the method is returned.
     * */
    fun getString(key: String, defaultValue: String): String {
        return data[key] ?: defaultValue
    }

    /**
     * Retrieve a int value for passed in matching key.
     * @param key custom data key
     * @param defaultValue default value if value for the key does not exist.
     * @return value for the key if it exist otherwise the default value passed
     * to the method is returned.
     * */
    fun getInt(key: String, defaultValue: Int): Int {
        return data[key]?.toInt() ?: defaultValue
    }

    /**
     * Retrieve a long value for passed in matching key.
     * @param key custom data key
     * @param defaultValue default value if value for the key does not exist.
     * @return value for the key if it exist otherwise the default value passed
     * to the method is returned.
     * */
    fun getLong(key: String, defaultValue: Long): Long {
        return data[key]?.toLong() ?: defaultValue
    }

    /**
     * Retrieve a boolean value for passed in matching key.
     * @param key custom data key
     * @param defaultValue default value if value for the key does not exist.
     * @return value for the key if it exist otherwise the default value passed
     * to the method is returned.
     * */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return data[key]?.toBoolean() ?: defaultValue
    }

    /**
     * Retrieve a double value for passed in matching key.
     * @param key custom data key
     * @param defaultValue default value if value for the key does not exist.
     * @return value for the key if it exist otherwise the default value passed
     * to the method is returned.
     * */
    fun getDouble(key: String, defaultValue: Double): Double {
        return data[key]?.toDouble() ?: defaultValue
    }

    /**
     * Retrieve a float value for passed in matching key.
     * @param key custom data key
     * @param defaultValue default value if value for the key does not exist.
     * @return value for the key if it exist otherwise the default value passed
     * to the method is returned.
     * */
    fun getFloat(key: String, defaultValue: Float): Float {
        return data[key]?.toFloat() ?: defaultValue
    }

    /** Create a new mutable instance of this class with the associated custom value data
     * attached.
     * @return new mutable instance.
     * */
    fun toMutableExtras(): MutableExtras {
        return MutableExtras(data.toMutableMap())
    }

    /** Converts the custom value extras stored in this extra class as a JSON string.
     * @return JSON string
     * */
    fun toJSONString(): String {
        return if (isEmpty()) {
            "{}"
        } else {
            JSONObject(map).toString()
        }
    }

    /** Creates a new JSONObject containing the associated custom data.
     * @return JSON string
     * */
    fun toJSONObject(): JSONObject {
        return if (isEmpty()) {
            JSONObject()
        } else {
            JSONObject(map)
        }
    }

    /** Get a copy of this instance with the associated custom data.
     * @return new extras instance
     * */
    open fun copy(): Extras {
        return Extras(data.toMap())
    }

    /** returns true if the extras object does not contain key value pairs. Otherwise false
     * @return returns true if the extras object does not contain key value pairs. Otherwise false
     * */
    fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    /** returns true if the extras object contains key value pairs. Otherwise false
     * @return returns true if the extras object contains key value pairs. Otherwise false
     * */
    fun isNotEmpty(): Boolean {
        return data.isNotEmpty()
    }

    /** Count of key/value pairs contained in this extras object.*/
    val size: Int
        get() {
            return data.size
        }

    /** The backing map that holds the custom key value pairs data.*/
    val map: Map<String, String>
        get() {
            return data.toMap()
        }

    override fun toString(): String {
        return toJSONString()
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