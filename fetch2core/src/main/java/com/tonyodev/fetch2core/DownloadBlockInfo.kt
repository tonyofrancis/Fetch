package com.tonyodev.fetch2core

import android.os.Parcel
import android.os.Parcelable

class DownloadBlockInfo : DownloadBlock {

    override var downloadId: Int = -1
    override var blockPosition: Int = -1
    override var startByte: Long = -1L
    override var endByte: Long = -1L
    override var downloadedBytes: Long = -1L

    override val progress: Int
        get() {
            return calculateProgress(downloadedBytes, endByte - startByte)
        }

    override fun copy(): DownloadBlock {
        val downloadBlockInfo = DownloadBlockInfo()
        downloadBlockInfo.downloadId = downloadId
        downloadBlockInfo.blockPosition = blockPosition
        downloadBlockInfo.startByte = startByte
        downloadBlockInfo.endByte = endByte
        downloadBlockInfo.downloadedBytes = downloadedBytes
        return downloadBlockInfo
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DownloadBlockInfo
        if (downloadId != other.downloadId) return false
        if (blockPosition != other.blockPosition) return false
        if (startByte != other.startByte) return false
        if (endByte != other.endByte) return false
        if (downloadedBytes != other.downloadedBytes) return false
        return true
    }

    override fun hashCode(): Int {
        var result = downloadId
        result = 31 * result + blockPosition
        result = 31 * result + startByte.hashCode()
        result = 31 * result + endByte.hashCode()
        result = 31 * result + downloadedBytes.hashCode()
        return result
    }

    override fun toString(): String {
        return "DownloadBlock(downloadId=$downloadId, blockPosition=$blockPosition, " +
                "startByte=$startByte, endByte=$endByte, downloadedBytes=$downloadedBytes)"
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(downloadId)
        dest.writeInt(blockPosition)
        dest.writeLong(startByte)
        dest.writeLong(endByte)
        dest.writeLong(downloadedBytes)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DownloadBlockInfo> {

        @Suppress("UNCHECKED_CAST")
        override fun createFromParcel(source: Parcel): DownloadBlockInfo {
            val downloadBlockInfo = DownloadBlockInfo()
            downloadBlockInfo.downloadId = source.readInt()
            downloadBlockInfo.blockPosition = source.readInt()
            downloadBlockInfo.startByte = source.readLong()
            downloadBlockInfo.endByte = source.readLong()
            downloadBlockInfo.downloadedBytes = source.readLong()
            return downloadBlockInfo
        }

        override fun newArray(size: Int): Array<DownloadBlockInfo?> {
            return arrayOfNulls(size)
        }

    }

}