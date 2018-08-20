package com.tonyodev.fetch2core

import android.os.Parcel
import android.os.Parcelable

open class MutableExtras(protected val mutableData: MutableMap<String, String> = mutableMapOf()) : Extras(mutableData) {

    fun putInt(key: String, value: Int) {
        mutableData[key] = value.toString()
    }

    fun putString(key: String, value: String) {
        mutableData[key] = value
    }

    fun putLong(key: String, value: Long) {
        mutableData[key] = value.toString()
    }

    fun putDouble(key: String, value: Double) {
        mutableData[key] = value.toString()
    }

    fun putFloat(key: String, value: Float) {
        mutableData[key] = value.toString()
    }

    fun putBoolean(key: String, value: Boolean) {
        mutableData[key] = value.toString()
    }

    fun clear() {
        mutableData.clear()
    }

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