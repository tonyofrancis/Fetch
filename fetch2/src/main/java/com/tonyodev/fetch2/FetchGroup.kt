package com.tonyodev.fetch2

import android.os.Parcelable
import java.io.Serializable

/**
 * An immutable object which contains a current snapshot of all the information
 * about a specific download group managed by Fetch.
 * */
interface FetchGroup: Parcelable, Serializable {

    /**
     * The group id.
     * */
    val id: Int

    /**
     * The Fetch namespace this group belongs to.
     * */
    val namespace: String

    /**
     * All downloads belonging to this group.
     * */
    val downloads: List<Download>

    /**
     * All queued downloads belonging to this group.
     * */
    val queuedDownloads: List<Download>

    /**
     * All added downloads belonging to this group.
     * */
    val addedDownloads: List<Download>

    /**
     * All paused downloads belonging to this group.
     * */
    val pausedDownloads: List<Download>

    /**
     * All downloading downloads belonging to this group.
     * */
    val downloadingDownloads: List<Download>

    /**
     * All completed downloads belonging to this group.
     * */
    val completedDownloads: List<Download>

    /**
     * All cancelled downloads belonging to this group.
     * */
    val cancelledDownloads: List<Download>

    /**
     * All failed downloads belonging to this group.
     * */
    val failedDownloads: List<Download>

    /**
     * All deleted downloads belonging to this group.
     * */
    val deletedDownloads: List<Download>

    /**
     * All removed downloads belonging to this group.
     * */
    val removedDownloads: List<Download>

}