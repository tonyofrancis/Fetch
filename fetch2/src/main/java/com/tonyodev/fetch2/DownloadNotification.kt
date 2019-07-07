package com.tonyodev.fetch2

import android.os.Parcel
import android.os.Parcelable
import com.tonyodev.fetch2.util.DEFAULT_INSTANCE_NAMESPACE
import java.io.Serializable

/** An object that represents a Fetch download notification.*/
open class DownloadNotification: Parcelable, Serializable {

    /** The download status*/
    var status: Status = Status.NONE

    /** Returns the progress of a download or -1 if the progress is unknown.*/
    var progress: Int = -1

    /** The notification id for this download.*/
    var notificationId = -1

    /** The group id this download belongs too*/
    var groupId = -1

    /** The estimated time the download will take to finish.
     * If -1, the eta is unknown or the download is not downloading.*/
    var etaInMilliSeconds = -1L

    /** The download bytes per second for this download. If -1 the download
     * is not downloading.*/
    var downloadedBytesPerSecond = -1L

    /** The total file bytes. -1 if unknown.*/
    var total = -1L

    /** The downloaded file bytes*/
    var downloaded = -1L

    /**
     * The Fetch namespace this download notification belongs too
     * */
    var namespace: String = DEFAULT_INSTANCE_NAMESPACE

    /** The notification title*/
    var title: String = ""

    /** Returns true if the download queued or is downloading.*/
    val isActive: Boolean
        get() {
            return status == Status.QUEUED || status == Status.DOWNLOADING
        }

    /** Returns true if the download paused.*/
    val isPaused: Boolean
        get() {
            return status == Status.PAUSED
        }

    /** Returns true if the download failed.*/
    val isFailed: Boolean
        get() {
            return status == Status.FAILED
        }

    /** Returns true if the download is complete.*/
    val isCompleted: Boolean
        get() {
            return status == Status.COMPLETED
        }

    /** Returns true if the download is downloading.*/
    val isDownloading: Boolean
        get() {
            return status == Status.DOWNLOADING
        }

    /** Returns true if the download was cancelled.*/
    val isCancelled: Boolean
        get() {
            return status == Status.CANCELLED
        }

    /** Returns true if the download was deleted.*/
    val isDeleted: Boolean
        get() {
            return status == Status.DELETED
        }

    /** Returns true if the download was removed.*/
    val isRemoved: Boolean
        get() {
            return status == Status.REMOVED
        }

    /** Returns true if the download queued.*/
    val isQueued: Boolean
        get() {
            return status == Status.QUEUED
        }

    /** Returns true if the download notification is ongoing.*/
    val isOnGoingNotification: Boolean
        get() {
            return when (status) {
                Status.QUEUED,
                Status.DOWNLOADING -> true
                else -> false
            }
        }

    /** Returns true if the download notification was cancelled.*/
    val isCancelledNotification: Boolean
        get() {
            return when (status) {
                Status.DELETED,
                Status.REMOVED,
                Status.CANCELLED -> true
                else -> false
            }
        }

    /** Returns true if the download notification is removable.*/
    val isRemovableNotification: Boolean
        get() {
            return when (status) {
                Status.COMPLETED,
                Status.FAILED,
                Status.CANCELLED,
                Status.REMOVED,
                Status.DELETED -> true
                else -> false
            }
        }

    /** Returns if the progress of a download is indeterminate.*/
    val progressIndeterminate: Boolean
        get() {
            return total == -1L
        }

    /** Download Action Types used by the FetchNotificationBroadcastReceiver.*/
    enum class ActionType {
        /* Paused an ongoing download.*/
        PAUSE,
        /** Resumes a paused download.*/
        RESUME,
        /* Cancels a download.*/
        CANCEL,
        /* Delete a download.**/
        DELETE,
        /** Retry failed downloads.*/
        RETRY,
        /** Pause ongoing downloads.*/
        PAUSE_ALL,
        /** Resume paused downloads.*/
        RESUME_ALL,
        /** Cancel downloads.*/
        CANCEL_ALL,
        /** Delete downloads.*/
        DELETE_ALL,
        /** Retry downloads.*/
        RETRY_ALL;
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(status.value)
        dest.writeInt(progress)
        dest.writeInt(notificationId)
        dest.writeInt(groupId)
        dest.writeLong(etaInMilliSeconds)
        dest.writeLong(downloadedBytesPerSecond)
        dest.writeLong(total)
        dest.writeLong(downloaded)
        dest.writeString(namespace)
        dest.writeString(title)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DownloadNotification
        if (status != other.status) return false
        if (progress != other.progress) return false
        if (notificationId != other.notificationId) return false
        if (groupId != other.groupId) return false
        if (etaInMilliSeconds != other.etaInMilliSeconds) return false
        if (downloadedBytesPerSecond != other.downloadedBytesPerSecond) return false
        if (total != other.total) return false
        if (downloaded != other.downloaded) return false
        if (namespace != other.namespace) return false
        if (title != other.title) return false
        return true
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + progress
        result = 31 * result + notificationId
        result = 31 * result + groupId
        result = 31 * result + etaInMilliSeconds.hashCode()
        result = 31 * result + downloadedBytesPerSecond.hashCode()
        result = 31 * result + total.hashCode()
        result = 31 * result + downloaded.hashCode()
        result = 31 * result + namespace.hashCode()
        result = 31 * result + title.hashCode()
        return result
    }

    override fun toString(): String {
        return "DownloadNotification(status=$status, progress=$progress, notificationId=$notificationId," +
                " groupId=$groupId, etaInMilliSeconds=$etaInMilliSeconds, downloadedBytesPerSecond=$downloadedBytesPerSecond, " +
                "total=$total, downloaded=$downloaded, namespace='$namespace', title='$title')"
    }

    companion object CREATOR : Parcelable.Creator<DownloadNotification> {

        override fun createFromParcel(source: Parcel): DownloadNotification {
            val status = Status.valueOf(source.readInt())
            val progress = source.readInt()
            val notificationId = source.readInt()
            val groupId = source.readInt()
            val etaInMilliSeconds = source.readLong()
            val downloadedBytesPerSeconds = source.readLong()
            val total = source.readLong()
            val downloaded = source.readLong()
            val namespace = source.readString() ?: ""
            val title = source.readString() ?: ""
            val downloadNotification = DownloadNotification()
            downloadNotification.status = status
            downloadNotification.progress = progress
            downloadNotification.notificationId = notificationId
            downloadNotification.groupId = groupId
            downloadNotification.etaInMilliSeconds = etaInMilliSeconds
            downloadNotification.downloadedBytesPerSecond = downloadedBytesPerSeconds
            downloadNotification.total = total
            downloadNotification.downloaded = downloaded
            downloadNotification.namespace = namespace
            downloadNotification.title = title
            return downloadNotification
        }

        override fun newArray(size: Int): Array<DownloadNotification?> {
            return arrayOfNulls(size)
        }

    }

}