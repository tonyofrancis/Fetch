package com.tonyodev.fetch2.exception

open class FetchImplementationException constructor(message: String,
                                                    val code: Code = Code.NONE)
    : RuntimeException(message) {

    enum class Code {
        NONE,
        LOGGER,
        CLOSED,
        ILLEGAL_CONCURRENT_INSERT,
        INVALID_STATUS,
        DOWNLOAD_NOT_FOUND;
    }

}