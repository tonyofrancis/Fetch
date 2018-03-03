package com.tonyodev.fetchmigrator.fetch1

internal interface FetchConst {

    companion object {

        /**
         * Status of a download request that could not be enqueued.
         */
        const val STATUS_NOT_QUEUED = -900

        /**
         * Status of a download request if it is queued for downloading.
         */
        const val STATUS_QUEUED = 900

        /**
         * Status of a download request if it is currently downloading.
         */
        const val STATUS_DOWNLOADING = 901

        /**
         * Status of a download request if it is paused.
         */
        const val STATUS_PAUSED = 902

        /**
         * Status of a download request if the file has been downloaded successfully.
         */
        const val STATUS_DONE = 903

        /**
         * Status of a download request if an error occurred during downloading.
         */
        const val STATUS_ERROR = 904

        /**
         * Status of a download request if it was successfully removed.
         */
        const val STATUS_REMOVED = 905

        /**
         * Priority type used to set a download request's download
         * priority to HIGH.
         */
        const val PRIORITY_HIGH = 601

        /**
         * Priority type used to set a download request's download
         * priority to NORMAL.
         */
        const val PRIORITY_NORMAL = 600

        /**
         * Error ID used when a download request fails because the local file could not be created
         * on the device or SD Card.
         */
        val ERROR_FILE_NOT_CREATED = ErrorUtils.FILE_NOT_CREATED

        /**
         * Error ID used when a download request fails because the application does not have
         * permission to write to the file path on the device or SD Card.
         */
        val ERROR_WRITE_PERMISSION_DENIED = ErrorUtils.WRITE_PERMISSION_DENIED

        /**
         * Error ID used when a download request fails because there is no storage space left of the
         * device or SD Card.
         */
        val ERROR_NO_STORAGE_SPACE = ErrorUtils.N0_STORAGE_SPACE

        /**
         * Error ID used when a download request fails because the requested download url could
         * not be found.
         */
        val ERROR_HTTP_NOT_FOUND = ErrorUtils.HTTP_NOT_FOUND

        /**
         * Error ID used when a download request fails because a successfully connection
         * could not be made with the server.
         */
        val ERROR_UNKNOWN_HOST = ErrorUtils.UNKNOWN_HOST

        /**
         * Error ID used when a download request fails because the connection timed out.
         */
        val ERROR_CONNECTION_TIMEOUT = ErrorUtils.CONNECTION_TIMED_OUT

        /**
         * Error ID used when a download request fails because of an unknown server error.
         */
        val ERROR_SERVER_ERROR = ErrorUtils.SERVER_ERROR

        /**
         * Error ID used when a download request fails because the local file is not found after a
         * download begins at the requested file path.
         */
        val ERROR_FILE_NOT_FOUND = ErrorUtils.FILE_NOT_FOUND

        /**
         * Error ID used when a download request is not queued because a request with the
         * local file path already exists in the FetchService database and is active.
         */
        val ERROR_REQUEST_ALREADY_EXIST = ErrorUtils.REQUEST_ALREADY_EXIST

        /**
         * Error ID used when a request could not be enqueued.
         */
        val ERROR_ENQUEUE_ERROR = ErrorUtils.ENQUEUE_ERROR

    }

}