package com.tonyodev.fetch2core

import android.os.Parcelable
import java.io.Serializable

/** Class used to hold partial downloaded information for a download.*/
interface DownloadBlock: Parcelable, Serializable {

    /* Download ID.*/
    val downloadId: Int

    /** Position in the downloading block sequence.*/
    val blockPosition: Int

    /** Block start position.*/
    val startByte: Long

    /* Block end position.*/
    val endByte: Long

    /* Downloaded bytes in block.*/
    val downloadedBytes: Long

    /** Progress completion of block.*/
    val progress: Int

    /** Copy DownloadBlock object.*/
    fun copy(): DownloadBlock

}