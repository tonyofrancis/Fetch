package com.tonyodev.fetch2

/**
 * Enumeration which contains specific errors that can occur.
 * */
enum class Error constructor(val value: Int) {

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

    /** Indicates that a Fetch instance already exist with the specified namespace.
     * This error is thrown by the FetchBuilder.
     * @see com.tonyodev.fetch2.Fetch.Builder
     * */
    FETCH_ALREADY_EXIST(12),

    /** Indicates that a request in the Fetch database already has this unique id.
     * Ids must be unique.*/
    REQUEST_WITH_ID_ALREADY_EXIST(13),

    /** Indicates that a request in the Fetch database already has this file path. File Paths
     * have to be unique for each request. This limitation maintains consistency and prevents data lose.
     * Fetch cannot write data to two different downloads with the same file path.
     * */
    REQUEST_WITH_FILE_PATH_ALREADY_EXIST(14),

    /** Indicates that unsuccessful response was returned by the server. */
    REQUEST_NOT_SUCCESSFUL(15);

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
                12 -> FETCH_ALREADY_EXIST
                13 -> REQUEST_WITH_ID_ALREADY_EXIST
                14 -> REQUEST_WITH_FILE_PATH_ALREADY_EXIST
                15 -> REQUEST_NOT_SUCCESSFUL
                else -> UNKNOWN
            }
        }

    }

}