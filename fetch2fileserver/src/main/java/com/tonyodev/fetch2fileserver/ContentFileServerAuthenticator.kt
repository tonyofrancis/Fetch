package com.tonyodev.fetch2fileserver

interface ContentFileServerAuthenticator {

    fun accept(authorization: String, contentFileRequest: ContentFileRequest): Boolean

}