package com.tonyodev.fetch2

import android.app.PendingIntent
import android.os.Parcelable
import com.tonyodev.fetch2.util.DEFAULT_GROUP_ID


class DownloadNotification(var download: Download) : Parcelable {

    var notificationId = download.id
    var groupId = DEFAULT_GROUP_ID
    var etaInMilliSeconds = -1L
    var downloadedBytesPerSecond = -1L
    var contentPendingIntent: PendingIntent? = null


    val isActive: Boolean
        get() {
            return download.status == Status.QUEUED || download.status == Status.DOWNLOADING
        }

    val isPaused: Boolean
        get() {
            return download.status == Status.PAUSED
        }

    val isFailed: Boolean
        get() {
            return download.status == Status.FAILED
        }

    val isCompleted: Boolean
        get() {
            return download.status == Status.COMPLETED
        }

    val isDownloading: Boolean
        get() {
            return download.status == Status.DOWNLOADING
        }

    val isCancelled: Boolean
        get() {
            return download.status == Status.CANCELLED
        }

    val isDeleted: Boolean
        get() {
            return download.status == Status.DELETED
        }

    val isRemoved: Boolean
        get() {
            return download.status == Status.REMOVED
        }

    val isQueued: Boolean
        get() {
            return download.status == Status.QUEUED
        }

    val isOnGoingNotification: Boolean
        get() {
            return when (download.status) {
                Status.QUEUED,
                Status.DOWNLOADING -> true
                else -> false
            }
        }

    val isCancelledNotification: Boolean
        get() {
            return when (download.status) {
                Status.DELETED,
                Status.REMOVED,
                Status.CANCELLED -> true
                else -> false
            }
        }

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

    val progress: Int
        get() {
            return download.progress
        }

    val progressIndeterminate: Boolean
        get() {
            return download.total == -1L
        }


    enum class ActionType {
        PAUSE,
        RESUME,
        CANCEL,
        DELETE
    }
}