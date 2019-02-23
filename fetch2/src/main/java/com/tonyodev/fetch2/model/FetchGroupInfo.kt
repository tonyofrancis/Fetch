package com.tonyodev.fetch2.model

import android.os.Parcel
import android.os.Parcelable
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.Status

class FetchGroupInfo(override val id: Int = 0,
                     override val namespace: String): FetchGroup {

    @Volatile
    override var downloads: List<Download> = emptyList()
        set(value) {
            field = value
            queuedDownloads = value.filter { it.status == Status.QUEUED }
            addedDownloads = value.filter { it.status == Status.ADDED }
            pausedDownloads = value.filter { it.status == Status.PAUSED }
            downloadingDownloads = value.filter { it.status == Status.DOWNLOADING }
            completedDownloads = value.filter { it.status == Status.COMPLETED }
            cancelledDownloads = value.filter { it.status == Status.CANCELLED }
            failedDownloads = value.filter { it.status == Status.FAILED }
            deletedDownloads = value.filter { it.status == Status.DELETED }
            removedDownloads = value.filter { it.status == Status.REMOVED }
        }

    override var queuedDownloads: List<Download> = emptyList()

    override var addedDownloads: List<Download> = emptyList()

    override var pausedDownloads: List<Download> = emptyList()

    override var downloadingDownloads: List<Download> = emptyList()

    override var completedDownloads: List<Download> = emptyList()

    override var cancelledDownloads: List<Download> = emptyList()

    override var failedDownloads: List<Download> = emptyList()

    override var deletedDownloads: List<Download> = emptyList()

    override var removedDownloads: List<Download> = emptyList()

    override val groupDownloadProgress: Int
        get() {
            val progressSum = downloads.sumBy { it.progress }
            return  progressSum / downloads.size
        }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(namespace)
        dest.writeSerializable(downloads as ArrayList)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FetchGroupInfo
        if (id != other.id) return false
        if (namespace != other.namespace) return false
        if (downloads != other.downloads) return false
        if (queuedDownloads != other.queuedDownloads) return false
        if (addedDownloads != other.addedDownloads) return false
        if (pausedDownloads != other.pausedDownloads) return false
        if (downloadingDownloads != other.downloadingDownloads) return false
        if (completedDownloads != other.completedDownloads) return false
        if (cancelledDownloads != other.cancelledDownloads) return false
        if (failedDownloads != other.failedDownloads) return false
        if (deletedDownloads != other.deletedDownloads) return false
        if (removedDownloads != other.removedDownloads) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + namespace.hashCode()
        result = 31 * result + downloads.hashCode()
        result = 31 * result + queuedDownloads.hashCode()
        result = 31 * result + addedDownloads.hashCode()
        result = 31 * result + pausedDownloads.hashCode()
        result = 31 * result + downloadingDownloads.hashCode()
        result = 31 * result + completedDownloads.hashCode()
        result = 31 * result + cancelledDownloads.hashCode()
        result = 31 * result + failedDownloads.hashCode()
        result = 31 * result + deletedDownloads.hashCode()
        result = 31 * result + removedDownloads.hashCode()
        return result
    }

    override fun toString(): String {
        return "FetchGroupInfo(id=$id, namespace='$namespace', downloads=$downloads)"
    }

    companion object CREATOR : Parcelable.Creator<FetchGroupInfo> {

        @Suppress("UNCHECKED_CAST")
        override fun createFromParcel(source: Parcel): FetchGroupInfo {
            val id = source.readInt()
            val namespace = source.readString() ?: ""
            val downloads = source.readSerializable() as ArrayList<Download>
            val fetchGroupInfo = FetchGroupInfo(id, namespace)
            fetchGroupInfo.downloads = downloads
            return fetchGroupInfo
        }

        override fun newArray(size: Int): Array<FetchGroupInfo?> {
            return arrayOfNulls(size)
        }

    }

}