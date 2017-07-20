/*
 * Copyright (C) 2017 Tonyo Francis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tonyodev.fetch;

/**
 * Constants used by Fetch and the FetchService.
 *
 * @author Tonyo Francis
 */

interface FetchConst {

    /**
     * Network type constant used by the FetchService to allow
     * downloading on any connection type. This includes wifi,
     * metered and un-metered connections.
     */
    int NETWORK_ALL = 200;

    /**
     * Network type constant used by the FetchService to allow
     * downloading only on a wifi connection.
     */
    int NETWORK_WIFI = 201;

    /**
     * Status of a download request that could not be enqueued.
     */
    int STATUS_NOT_QUEUED = -900;

    /**
     * Status of a download request if it is queued for downloading.
     * */
    int STATUS_QUEUED = 900;

    /**
     * Status of a download request if it is currently downloading.
     * */
    int STATUS_DOWNLOADING = 901;

    /**
     * Status of a download request if it is paused.
     * */
    int STATUS_PAUSED = 902;

    /**
     * Status of a download request if the file has been downloaded successfully.
     * */
    int STATUS_DONE  = 903;

    /**
     * Status of a download request if an error occurred during downloading.
     * */
    int STATUS_ERROR = 904;

    /**
     * Status of a download request if it was successfully removed.
     * */
    int STATUS_REMOVED = 905;

    /**
     * Priority type used to set a download request's download
     * priority to HIGH.
     * */
    int PRIORITY_HIGH = 601;

    /**
     * Priority type used to set a download request's download
     * priority to NORMAL.
     * */
    int PRIORITY_NORMAL = 600;

    /**
     * Error ID used when a download request fails for an unknown reason.
     * */
    int ERROR_UNKNOWN = ErrorUtils.UNKNOWN;

    /**
     * Error ID used when a download request fails because the local file could not be created
     * on the device or SD Card.
     * */
    int ERROR_FILE_NOT_CREATED = ErrorUtils.FILE_NOT_CREATED;

    /**
     * Error ID used when a download request fails because the application does not have
     * permission to write to the file path on the device or SD Card.
     * */
    int ERROR_WRITE_PERMISSION_DENIED = ErrorUtils.WRITE_PERMISSION_DENIED;

    /**
     * Error ID used when a download request fails because there is no storage space left of the
     * device or SD Card.
     * */
    int ERROR_NO_STORAGE_SPACE = ErrorUtils.N0_STORAGE_SPACE;

    /**
     * Error ID used when a download request fails because the requested download url could
     * not be found.
     * */
    int ERROR_HTTP_NOT_FOUND = ErrorUtils.HTTP_NOT_FOUND;

    /**
     * Error ID used when a download request fails because a successfully connection
     * could not be made with the server.
     * */
    int ERROR_UNKNOWN_HOST = ErrorUtils.UNKNOWN_HOST;

    /**
     * Error ID used when a download request fails because the connection timed out.
     * */
    int ERROR_CONNECTION_TIMEOUT = ErrorUtils.CONNECTION_TIMED_OUT;

    /**
     * Error ID used when a download request fails when an IllegalStateException
     * is thrown.
     * */
    int ERROR_ILLEGAL_STATE = ErrorUtils.ILLEGAL_STATE;

    /**
     * Error ID used when a download request fails because of an unknown server error.
     * */
    int ERROR_SERVER_ERROR = ErrorUtils.SERVER_ERROR;

    /**
     * Error ID used when a download request fails because the local file is not found after a
     * download begins at the requested file path.
     * */
    int ERROR_FILE_NOT_FOUND = ErrorUtils.FILE_NOT_FOUND;

    /**
     * Error ID used when a download request fails because a file already exist at the
     * requested file path.
     * */
    int ERROR_FILE_ALREADY_CREATED = ErrorUtils.FILE_ALREADY_CREATED;

    /**
     * Error ID used when a download request is not queued because a request with the
     * local file path already exists in the FetchService database and is active.
     * */
    int ERROR_REQUEST_ALREADY_EXIST = ErrorUtils.REQUEST_ALREADY_EXIST;

    /**
     * Error ID used when enqueuing a bad request.
     * */
    int ERROR_BAD_REQUEST = ErrorUtils.BAD_REQUEST;

    /**
     * Error ID used when a request could not be enqueued.
     * */
    int ERROR_ENQUEUE_ERROR = ErrorUtils.ENQUEUE_ERROR;

    /**
     * Default empty value of a Field.
     * */
    int DEFAULT_EMPTY_VALUE = DatabaseHelper.EMPTY_COLUMN_VALUE;

    /**
     * Default concurrent downloads limit.
     * */
    int DEFAULT_DOWNLOADS_LIMIT = 1;

    /**
     * Default ms interval for the call to "onUpdate".
     * */
    long DEFAULT_ON_UPDATE_INTERVAL = 2000;

    /**
     * Max concurrent downloads limit.
     * @deprecated Use your best judgement
     * */
    int MAX_DOWNLOADS_LIMIT = 7;

    /** indicates that no errors occurred*/
    int NO_ERROR =  -1;

    /** The ID value that is returned when a request was not enqueued*/
    int ENQUEUE_ERROR_ID = -1;
}
