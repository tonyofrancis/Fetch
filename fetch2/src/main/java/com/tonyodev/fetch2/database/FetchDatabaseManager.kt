package com.tonyodev.fetch2.database

import com.tonyodev.fetch2.PrioritySort
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.Logger
import java.io.Closeable

/**
 * This interface can be implemented by a class to create a custom FetchDatabaseManager.
 * The default Fetch Database Manager is FetchDatabaseManagerImpl which uses sqlite/room
 * to store download information. All methods and fields will be called on Fetch's background thread.
 * */
interface FetchDatabaseManager<T: DownloadInfo> : Closeable {

    /**
     * Checks if the database is closed.
     * */
    val isClosed: Boolean
    /**
     * Logger to be used
     * */
    val logger: Logger
    /**
     * Delegate used by Fetch to delete temporary files used to assist the parallel downloader.
     * This field is set by the Fetch Builder directly. Your implemention should set this to null
     * by default.
     * */
    var delegate: Delegate<T>?

    /**
     * Inserts a new download into the database.
     * @param downloadInfo object containing the download information.
     * @return a pair with the store or updated download information and a boolean.
     * indicated if the insert was a success.
     * */
    fun insert(downloadInfo: T): Pair<T, Boolean>

    /**
     * Inserts a list of downloads into the database.
     * @param downloadInfoList list objects containing the download information.
     * @return a list of pairs with the store or updated download information and a boolean.
     * indicated if the insert for each download was a success.
     * */
    fun insert(downloadInfoList: List<T>): List<Pair<T, Boolean>>

    /**
     * Deletes a download from the database.
     * @param downloadInfo object containing the download information.
     * */
    fun delete(downloadInfo: T)

    /**
     * Deletes a list of downloads from the database.
     * @param downloadInfoList list of objects containing the download information.
     * */
    fun delete(downloadInfoList: List<T>)

    /**
    * Deletes all downloads in the database.
    * */
    fun deleteAll()

    /**
     * Updates a download in the database.
     * @param downloadInfo the download information.
     * */
    fun update(downloadInfo: T)

    /**
     * Updates a list of downloads in the database.
     * @param downloadInfoList list of downloads.
     * */
    fun update(downloadInfoList: List<T>)

    /**
     * Updates only the file bytes and status of a download in the database.
     * @param downloadInfo the download information.
     * */
    fun updateFileBytesInfoAndStatusOnly(downloadInfo: T)

    /**
     * Gets a list of all the downloads in the database.
     * */
    fun get(): List<T>

    /**
     * Gets a download from the database by id if it exists.
     * @param id download id.
     * @return the download or null.
     * */
    fun get(id: Int): T?

    /**
     * Gets a list of downloads from the database by id if it exists.
     * @param ids the list ids the database will search against.
     * @return the list of downloads for each of ids in order. If a download
     * does not exist for an id null will be returned.
     * */
    fun get(ids: List<Int>): List<T?>

    /**
     * Gets a download by file name if it exists.
     * @param file the file
     * @return the download if it exists.
     * */
    fun getByFile(file: String): T?

    /**
     * Get all downloads by the specified status.
     * @param status the query status.
     * @return all downloads in the database with the specified status.
     * */
    fun getByStatus(status: Status): List<T>

    /**
     * Get all downloads by the specified statuses.
     * @param statuses the query statuses list.
     * @return all downloads in the database with the specified statuses.
     * */
    fun getByStatus(statuses: List<Status>): List<T>

    /**
     * Gets all downloads that belongs to the specified group.
     * @param group the group id
     * @return list of downloads belonging to the group
     * */
    fun getByGroup(group: Int): List<T>

    /**
     * Gets all downloads in the specified group with the specified statuses.
     * @param groupId the group id
     * @param statuses the list of statues to query against.
     * @return list of downloads matching the query.
     * */
    fun getDownloadsInGroupWithStatus(groupId: Int, statuses: List<Status>): List<T>

    /**
     * Get a list of downloads by the specified request identifier.
     * @param identifier the request identifier
     * @return list of downloads matching the query.
     * */
    fun getDownloadsByRequestIdentifier(identifier: Long): List<T>

    /**
     * Get a list of downloads by the specified tag.
     * @param tag the tag.
     * @return list of downloads matching the query.
     * */
    fun getDownloadsByTag(tag: String): List<T>

    /**
     * Gets a list of the ids of all groups managed my this fetch namespace.
     * @return a list of all groupIDs found in the database.
     * */
    fun getAllGroupIds(): List<Int>

    /**
     * Get a list of downloads that are pending(status = Queued) for download in sorted order by(priority(DESC), created(ASC)
     * @param prioritySort the sort priority for created. Default is ASC
     * @return list of pending downloads in sorted order.
     * */
    fun getPendingDownloadsSorted(prioritySort: PrioritySort): List<T>

    /**
     * Called when the first instance of Fetch for a namespace is created. Use this method
     * to ensure the database is clean and up to date.
     * Note: Applications may quit unexpectedly. Use this method to update the status of any downloads
     * with a status of Downloading to Queued. Otherwise these downloads will never continue.
     * */
    fun sanitizeOnFirstEntry()

    /**
     * Updates the extras on a download.
     * @param id the download id.
     * @param extras the new extras that will replace the existing extras on the download.
     * */
    fun updateExtras(id: Int, extras: Extras): T?

    /**
     * Gets the count/sum of all downloads with the status of Queued and Downloading combined.
     * @param includeAddedDownloads if to include downloads with the status of Added.
     * Added downloads are not considered pending by default.
     * @return the pending download count.
     * */
    fun getPendingCount(includeAddedDownloads: Boolean): Long

    /**
     * Get a new instance of DownloadInfo.
     * Note: Be sure to override DownloadInfo create parcelable and copy methods.
     * */
    fun getNewDownloadInfoInstance(): T

    /**
     * Interface used for the DownloadManager's delegate.
     * */
    interface Delegate<T: DownloadInfo> {

        /**
         * Deletes all associated temp files used to perform a download for a download.
         * @param downloadInfo download information.
         * */
        fun deleteTempFilesForDownload(downloadInfo: T)

    }

}