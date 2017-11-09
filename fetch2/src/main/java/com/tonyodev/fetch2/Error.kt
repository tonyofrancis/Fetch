package com.tonyodev.fetch2

enum class Error private constructor(val value: Int) {

    UNKNOWN(-1),
    NONE(0),
    REQUEST_ALREADY_EXIST(1),
    FILE_NOT_CREATED(2),
    THREAD_INTERRUPTED(3),
    DOWNLOAD_INTERRUPTED(4),
    CONNECTION_TIMED_OUT(5),
    HTTP_NOT_FOUND(6),
    UNKNOWN_HOST(7),
    WRITE_PERMISSION_DENIED(8),
    NO_STORAGE_SPACE(9),
    SERVER_ERROR(10),
    UNSUCCESSFUL_CONNECTION(11),
    NO_NETWORK_CONNECTION(12),
    BAD_URL(13),
    BAD_FILE_PATH(14),
    INVALID_SERVER_RESPONSE(15);


    override fun toString(): String {
        return "Error Code: " + value
    }

    companion object {

        fun valueOf(error: Int): Error {
            when (error) {
                -1 -> return UNKNOWN
                0 -> return NONE
                1 -> return REQUEST_ALREADY_EXIST
                2 -> return FILE_NOT_CREATED
                3 -> return THREAD_INTERRUPTED
                4 -> return DOWNLOAD_INTERRUPTED
                5 -> return CONNECTION_TIMED_OUT
                6 -> return HTTP_NOT_FOUND
                7 -> return UNKNOWN_HOST
                8 -> return WRITE_PERMISSION_DENIED
                9 -> return NO_STORAGE_SPACE
                10 -> return SERVER_ERROR
                11 -> return UNSUCCESSFUL_CONNECTION
                12 -> return NO_NETWORK_CONNECTION
                13 -> return BAD_URL
                14 -> return BAD_FILE_PATH
                15 -> return INVALID_SERVER_RESPONSE
                else -> return UNKNOWN
            }
        }
    }
}