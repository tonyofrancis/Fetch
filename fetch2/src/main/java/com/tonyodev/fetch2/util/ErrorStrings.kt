@file:JvmName("FetchErrorStrings")

package com.tonyodev.fetch2.util

const val DOWNLOAD_NOT_FOUND = "fetch download not found"
const val FAILED_TO_ENQUEUE_REQUEST = "UNIQUE constraint failed: requests._id (code 1555)"
const val EMPTY_RESPONSE_BODY = "empty_response_body"
const val RESPONSE_NOT_SUCCESSFUL = "request_not_successful"
const val UNKNOWN_ERROR = "unknown"
const val FNC = "FNC"
const val ENOENT = "open failed: ENOENT (No such file or directory)"
const val ETIMEDOUT = "recvfrom failed: ETIMEDOUT (Connection timed out)"
const val IO404 = "java.io.IOException: 404"
const val NO_ADDRESS_HOSTNAME = "No address associated with hostname"
const val CONNECTION_TIMEOUT = "timeout"
const val HOST_RESOLVE_ISSUE = "Unable to resolve host"
const val EACCES = "open failed: EACCES (Permission denied)"
const val ENOSPC = "write failed: ENOSPC (No space left on device)"
const val DATABASE_DISK_FULL = "database or disk is full (code 13)"
const val FETCH_DATABASE_ERROR = "Fetch data base error"
const val FETCH_ALREADY_EXIST = "already exists. You cannot have more than one active " +
        "instance of Fetch with the same namespace. Did your forget to call close the old instance?"
const val UNIQUE_ID_DATABASE = "UNIQUE constraint failed: requests._id"
const val UNIQUE_FILE_PATH_DATABASE = "UNIQUE constraint failed: requests._file"
const val FAILED_TO_CONNECT = "Failed to connect"