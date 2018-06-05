package com.tonyodev.fetch2fileserver

abstract class AbstractFetchFileServerAuthenticator : FetchFileServerAuthenticator {

    override fun accept(authorization: String, fileRequest: FileRequest): Boolean {
        return true
    }

}