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
        DOWNLOAD_NOT_FOUND;
    }

}