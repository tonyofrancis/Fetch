package com.tonyodev.fetch2fileserver

interface FetchFileServerAuthenticator {

    fun accept(authorization: String, fileRequest: FileRequest): Boolean

}