package com.tonyodev.fetch2.exception

open class FetchException constructor(message: String,
                                      val code: Code = Code.NONE) : RuntimeException(message) {

    enum class Code {
        NONE,
        INITIALIZATION,
        INCOMPLETE_INITIALIZATION,
        ILLEGAL_ARGUMENT,
        CLOSED,
        EMPTY_RESPONSE_BODY,
        UNKNOWN,
        REQUEST_NOT_SUCCESSFUL,
        FETCH_INSTANCE_WITH_NAMESPACE_ALREADY_EXIST,
        LOGGER,
        ILLEGAL_CONCURRENT_INSERT,
        INVALID_STATUS,
        DOWNLOAD_NOT_FOUND,
        GLOBAL_CONFIGURATION_NOT_SET,
        INVALID_CONTENT_MD5,
        REQUEST_WITH_FILE_PATH_ALREADY_EXIST,
        DOWNLOAD_INCOMPLETE,
        REQUEST_NOT_UPDATED,
        COMPLETED_DOWNLOAD_NOT_ADDED,
        FILE_SERVER_DOWNLOADER_NOT_SET;
    }

}