package com.tonyodev.fetch2

import com.tonyodev.fetch2core.Downloader

/**
 * Enumeration which contains specific errors that can occur.
 * */
enum class Error constructor(
        /** Error Value*/
        val value: Int,
        /** A throwable will only be present at the time the error
         * occurs and will not be saved in the Fetch database. This means that if a download encounters
         * an error, the throwable will be attached to the error and the error will be attached to the
         * download and this information will be sent to all attached FetchListeners.
         * Fetch will not save the throwable in the database for later use.
         * Only the error enum type and value field of the error will be saved. This throwable field may be null.
         * */
        var throwable: Throwable? = null,

        /** The http response containing the reason the error may have occured. It will only be present
         * at the time the error occurs and will not be saved in the Fetch database.
         * This httpResponse may be null.*/
        var httpResponse: Downloader.Response? = null) {

    /** Indicates that the specific issue or error is not known.*/
    UNKNOWN(-1),

    /** Indicates that no error has occurred. This is the default error for a download
     * when no errors have occurred.*/
    NONE(0),

    /** Indicates that Fetch was not able to create the file on disk. This may indicate
     * that the app does not have the proper read and write permissions.*/
    FILE_NOT_CREATED(1),

    /** Indicates that the connection has timed out.*/
    CONNECTION_TIMED_OUT(2),

    /** Indicates that the download client was not able to identify or find the host.
     * This may indicate that the url of a download may be bad.*/
    UNKNOWN_HOST(3),

    /** Indicates that the download client was not able to find the url.
     * This may indicate that the url of a download may be broken.*/
    HTTP_NOT_FOUND(4),

    /** Indicates that the app does not have proper read and write permissions.*/
    WRITE_PERMISSION_DENIED(5),

    /** Indicates that the device has no more storage space to continue the download.*/
    NO_STORAGE_SPACE(6),

    /** Indicates that the devices does not have an active internet connection.*/
    NO_NETWORK_CONNECTION(7),

    /** Indicates that an empty response was returned by the server.*/
    EMPTY_RESPONSE_FROM_SERVER(8),

    /** Indicates that Fetch was unable to enqueue a new request because the same request
     * already exist and is being managed.*/
    REQUEST_ALREADY_EXIST(9),

    /** Indicates that Fetch does not manage a download with the specified id.*/
    DOWNLOAD_NOT_FOUND(10),

    /** Indicates that a Fetch database error occurred.*/
    FETCH_DATABASE_ERROR(11),

    /** Error 12 no longer needed by Fetch. removed*/

    /** Indicates that a request in the Fetch database already has this unique id.
     * Ids must be unique.*/
    REQUEST_WITH_ID_ALREADY_EXIST(13),

    /** Indicates that a request in the Fetch database already has this file path. File Paths
     * have to be unique for each request. This limitation maintains consistency and prevents data lose.
     * Fetch cannot write data to two different downloads with the same file path.
     * */
    REQUEST_WITH_FILE_PATH_ALREADY_EXIST(14),

    /** Indicates that unsuccessful response was returned by the server. */
    REQUEST_NOT_SUCCESSFUL(15),

    /** Indicates that an unknown IO issue occurred. */
    UNKNOWN_IO_ERROR(16),

    /** Indicates that the file belonging to the request has been deleted. The file
     * could have been deleted by an external source.*/
    FILE_NOT_FOUND(17),

    /** Error 18 no longer needed by Fetch. removed*/

    /** Indicates that the request url is not a valid url to reach a Fetch File Server.*/
    FETCH_FILE_SERVER_URL_INVALID(19),

    /** Indicates that the downloaded file hash does not match the hash the server returned
     * for the content.*/
    INVALID_CONTENT_HASH(20),

    /** Indicates that Fetch was unable to update the existing request.*/
    FAILED_TO_UPDATE_REQUEST(21),

    /** Indicates that Fetch was unable to add a completed download.*/
    FAILED_TO_ADD_COMPLETED_DOWNLOAD(22),

    /** Indicates that the Fetch File Server returned the wrong response type. */
    FETCH_FILE_SERVER_INVALID_RESPONSE(23),

    /** Indicates that the Request Being Queried does not exist.*/
    REQUEST_DOES_NOT_EXIST(24),

    /** Indicates that the Request was not enqueued.*/
    ENQUEUE_NOT_SUCCESSFUL(25),

    /** Indicates that the Completed download was not added successfully.*/
    COMPLETED_NOT_ADDED_SUCCESSFULLY(26),

    /** Indicates that the requests in the list are not distinct by file name.*/
    ENQUEUED_REQUESTS_ARE_NOT_DISTINCT(27),

    /** Indicates that the operation to rename the file for a download failed because
     * the download does not have a status of complete.*/
    FAILED_TO_RENAME_INCOMPLETE_DOWNLOAD_FILE(28),

    /**
     * Indicates that the operation to rename the file failed for some reason.
     * */
    FAILED_TO_RENAME_FILE(29),

    /**
     * Indicates that an error occured when pre allocating the needed space on the storage system for the download.
     * */
    FILE_ALLOCATION_FAILED(30);

    companion object {

        @JvmStatic
        fun valueOf(value: Int): Error {
            return when (value) {
                -1 -> UNKNOWN
                0 -> NONE
                1 -> FILE_NOT_CREATED
                2 -> CONNECTION_TIMED_OUT
                3 -> UNKNOWN_HOST
                4 -> HTTP_NOT_FOUND
                5 -> WRITE_PERMISSION_DENIED
                6 -> NO_STORAGE_SPACE
                7 -> NO_NETWORK_CONNECTION
                8 -> EMPTY_RESPONSE_FROM_SERVER
                9 -> REQUEST_ALREADY_EXIST
                10 -> DOWNLOAD_NOT_FOUND
                11 -> FETCH_DATABASE_ERROR
                13 -> REQUEST_WITH_ID_ALREADY_EXIST
                15 -> REQUEST_NOT_SUCCESSFUL
                16 -> UNKNOWN_IO_ERROR
                17 -> FILE_NOT_FOUND
                19 -> FETCH_FILE_SERVER_URL_INVALID
                20 -> INVALID_CONTENT_HASH
                21 -> FAILED_TO_UPDATE_REQUEST
                22 -> FAILED_TO_ADD_COMPLETED_DOWNLOAD
                23 -> FETCH_FILE_SERVER_INVALID_RESPONSE
                24 -> REQUEST_DOES_NOT_EXIST
                25 -> ENQUEUE_NOT_SUCCESSFUL
                26 -> COMPLETED_NOT_ADDED_SUCCESSFULLY
                27 -> ENQUEUED_REQUESTS_ARE_NOT_DISTINCT
                28 -> FAILED_TO_RENAME_INCOMPLETE_DOWNLOAD_FILE
                29 -> FAILED_TO_RENAME_FILE
                30 -> FILE_ALLOCATION_FAILED
                else -> UNKNOWN
            }
        }

    }

}