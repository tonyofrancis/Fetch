package com.tonyodev.fetch2

import android.app.PendingIntent
import android.os.Parcel
import android.os.Parcelable
import com.tonyodev.fetch2.database.DownloadInfo

/** An object that represents a Fetch download notification.*/
class DownloadNotification(download: Download) : Parcelable {

    /*The download the download notification object represents.*/
    var download: Download = download
        set(value) {
            notificationId = download.id
            groupId = download.group
            field = value
        }

    /** The notification id for this download.*/
    var notificationId = download.id

    /** The group id this download belongs too*/
    var groupId = download.group

    /** The estimated time the download will take to finish.
     * If -1, the eta is unknown or the download is not downloading.*/
    var etaInMilliSeconds = -1L

    /** The download bytes per second for this download. If -1 the download
     * is not downloading.*/
    var downloadedBytesPerSecond = -1L

    /** The pending intent that will launch a component when the notification
     * associated with the download is tapped by the user.*/
    var contentPendingIntent: PendingIntent? = null

    /** Returns true if the download queued or is downloading.*/
    val isActive: Boolean
        get() {
            return download.status == Status.QUEUED || download.status == Status.DOWNLOADING
        }

    /** Returns true if the download paused.*/
    val isPaused: Boolean
        get() {
            return download.status == Status.PAUSED
        }

    /** Returns true if the download failed.*/
    val isFailed: Boolean
        get() {
            return download.status == Status.FAILED
        }

    /** Returns true if the download is complete.*/
    val isCompleted: Boolean
        get() {
            return download.status == Status.COMPLETED
        }

    /** Returns true if the download is downloading.*/
    val isDownloading: Boolean
        get() {
            return download.status == Status.DOWNLOADING
        }

    /** Returns true if the download was cancelled.*/
    val isCancelled: Boolean
        get() {
            return download.status == Status.CANCELLED
        }

    /** Returns true if the download was deleted.*/
    val isDeleted: Boolean
        get() {
            return download.status == Status.DELETED
        }

    /** Returns true if the download was removed.*/
    val isRemoved: Boolean
        get() {
            return download.status == Status.REMOVED
        }

    /** Returns true if the download queued.*/
    val isQueued: Boolean
        get() {
            return download.status == Status.QUEUED
        }

    /** Returns true if the download notification is ongoing.*/
    val isOnGoingNotification: Boolean
        get() {
            return when (download.status) {
                Status.QUEUED,
                Status.DOWNLOADING -> true
                else -> false
            }
        }

    /** Returns true if the download notification was cancelled.*/
    val isCancelledNotification: Boolean
        get() {
            return when (download.status) {
                Status.DELETED,
                Status.REMOVED,
                Status.CANCELLED -> true
                else -> false
            }
        }

    /** Returns true if the download notification is removable.*/
    val isRemovableNotification: Boolean
        get() {
            return when (download.status) {
                Status.COMPLETED,
                Status.FAILED,
                Status.CANCELLED,
                Status.REMOVED,
                Status.DELETED -> true
                else -> false
            }
        }

    /** Returns the progress of a download or -1 if the progress is unknown.*/
    val progress: Int
        get() {
            return download.progress
        }

    /** Returns if the progress of a download is indeterminate.*/
    val progressIndeterminate: Boolean
        get() {
            return download.total == -1L
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
        RETRY;
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeParcelable(download, flags)
        dest?.writeInt(notificationId)
        dest?.writeInt(groupId)
        dest?.writeLong(etaInMilliSeconds)
        dest?.writeLong(downloadedBytesPerSecond)
        dest?.writeParcelable(contentPendingIntent, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DownloadNotification
        if (download != other.download) return false
        if (notificationId != other.notificationId) return false
        if (groupId != other.groupId) return false
        if (etaInMilliSeconds != other.etaInMilliSeconds) return false
        if (downloadedBytesPerSecond != other.downloadedBytesPerSecond) return false
        if (contentPendingIntent != other.contentPendingIntent) return false
        return true
    }

    override fun hashCode(): Int {
        var result = download.hashCode()
        result = 31 * result + notificationId
        result = 31 * result + groupId
        result = 31 * result + etaInMilliSeconds.hashCode()
        result = 31 * result + downloadedBytesPerSecond.hashCode()
        result = 31 * result + (contentPendingIntent?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "DownloadNotification(download=$download, notificationId=$notificationId, " +
                "groupId=$groupId, etaInMilliSeconds=$etaInMilliSeconds, " +
                "downloadedBytesPerSecond=$downloadedBytesPerSecond, contentPendingIntent=$contentPendingIntent)"
    }

    companion object CREATOR : Parcelable.Creator<DownloadNotification> {

        override fun createFromParcel(source: Parcel): DownloadNotification {
            val download = source.readParcelable(DownloadInfo::class.java.classLoader) as Download
            val notificationId = source.readInt()
            val groupId = source.readInt()
            val etaInMilliSeconds = source.readLong()
            val downloadedBytesPerSeconds = source.readLong()
            val contentPendingIntent = source.readParcelable<PendingIntent?>(PendingIntent::class.java.classLoader)
            val downloadNotification = DownloadNotification(download)
            downloadNotification.notificationId = notificationId
            downloadNotification.groupId = groupId
            downloadNotification.etaInMilliSeconds = etaInMilliSeconds
            downloadNotification.downloadedBytesPerSecond = downloadedBytesPerSeconds
            downloadNotification.contentPendingIntent = contentPendingIntent
            return downloadNotification
        }

        override fun newArray(size: Int): Array<DownloadNotification?> {
            return arrayOfNulls(size)
        }

    }

}