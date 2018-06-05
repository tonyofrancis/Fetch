package com.tonyodev.fetch2fileserver

abstract class AbstractContentFileServerAuthenticator : ContentFileServerAuthenticator {

    override fun accept(authorization: String, contentFileRequest: ContentFileRequest): Boolean {
        return true
    }

}