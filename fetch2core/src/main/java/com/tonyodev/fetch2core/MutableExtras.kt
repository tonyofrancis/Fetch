package com.tonyodev.fetch2core

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

/**
 *  Mutable extras class that sets and holds custom key/value pair data for a request and download.
 *  Use this class to update/store custom information that belongs to a request/download.
 *  Save this extra on a fetch instance with the matching namespace for the request/download.
 *  @see com.tonyodev.fetch2.Fetch.replaceExtras(id, extras, func, func) method.
 * */
open class MutableExtras(protected val mutableData: MutableMap<String, String> = mutableMapOf())
    : Extras(mutableData), Serializable {

    /**
     * Stores a int value with its key inside this extras object.
     * @param key key used to retrieve value.
     * @param value int value data
     * */
    fun putInt(key: String, value: Int) {
        mutableData[key] = value.toString()
    }

    /**
     * Stores a string value with its key inside this extras object.
     * @param key key used to retrieve value.
     * @param value string value data
     * */
    fun putString(key: String, value: String) {
        mutableData[key] = value
    }

    /**
     * Stores a long value with its key inside this extras object.
     * @param key key used to retrieve value.
     * @param value long value data
     * */
    fun putLong(key: String, value: Long) {
        mutableData[key] = value.toString()
    }

    /**
     * Stores a double value with its key inside this extras object.
     * @param key key used to retrieve value.
     * @param value double value data
     * */
    fun putDouble(key: String, value: Double) {
        mutableData[key] = value.toString()
    }

    /**
     * Stores a float value with its key inside this extras object.
     * @param key key used to retrieve value.
     * @param value float value data
     * */
    fun putFloat(key: String, value: Float) {
        mutableData[key] = value.toString()
    }

    /**
     * Stores a boolean value with its key inside this extras object.
     * @param key key used to retrieve value.
     * @param value boolean value data
     * */
    fun putBoolean(key: String, value: Boolean) {
        mutableData[key] = value.toString()
    }

    /** Clears all key value data stored in this extras object.*/
    fun clear() {
        mutableData.clear()
    }

    /** Returns an immutable copy of this instance.
     * @return Returns an immutable copy of this instance.
     * */
    fun toExtras(): Extras {
        return Extras(mutableData.toMap())
    }

    override fun toString(): String {
        return toJSONString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(HashMap(mutableData))
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        other as MutableExtras
        if (mutableData != other.mutableData) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mutableData.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<MutableExtras> {

        @Suppress("UNCHECKED_CAST")
        override fun createFromParcel(source: Parcel): MutableExtras {
            return MutableExtras((source.readSerializable() as HashMap<String, String>).toMutableMap())
        }

        override fun newArray(size: Int): Array<MutableExtras?> {
            return arrayOfNulls(size)
        }
    }

}