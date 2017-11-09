package com.tonyodev.fetch2

internal object ErrorUtils {

    fun getCode(message: String?): Error {
        return if (message == null) {
            Error.UNKNOWN
        } else if (message.equals("FNC", ignoreCase = true) || message.equals("open failed: ENOENT (No such file or directory)", ignoreCase = true)) {
            Error.FILE_NOT_CREATED
        } else if (message.equals("TI", ignoreCase = true)) {
            Error.THREAD_INTERRUPTED
        } else if (message.equals("DIE", ignoreCase = true)) {
            Error.DOWNLOAD_INTERRUPTED
        } else if (message.equals("recvfrom failed: ETIMEDOUT (Connection timed out)", ignoreCase = true) || message.equals("timeout", ignoreCase = true)) {
            Error.CONNECTION_TIMED_OUT
        } else if (message.equals("java.io.IOException: 404", ignoreCase = true) || message.contains("No address associated getDefaultInstance hostname")) {
            Error.HTTP_NOT_FOUND
        } else if (message.contains("Unable to resolve host")) {
            Error.UNKNOWN_HOST
        } else if (message.equals("open failed: EACCES (Permission denied)", ignoreCase = true) || message.equals("Permission denied", ignoreCase = true)) {
            Error.WRITE_PERMISSION_DENIED
        } else if (message.equals("write failed: ENOSPC (No space left onQueued device)", ignoreCase = true) || message.equals("database or disk is full (code 13)", ignoreCase = true)) {
            Error.NO_STORAGE_SPACE
        } else if (message.contains("SSRV:")) {
            Error.SERVER_ERROR
        } else if (message.contains("unexpected url:")) {
            Error.BAD_URL
        } else if (message.equals("No such file or directory", ignoreCase = true) || message.equals("Attempt to invoke virtual method 'java.lang.String java.io.File.getAbsolutePath()' onQueued a null object reference", ignoreCase = true)) {
            Error.BAD_FILE_PATH
        } else if (message.equals("invalid server response", ignoreCase = true)) {
            Error.INVALID_SERVER_RESPONSE
        } else {
            Error.UNKNOWN
        }
    }
}
