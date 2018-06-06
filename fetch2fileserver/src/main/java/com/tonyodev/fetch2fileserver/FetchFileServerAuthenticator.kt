package com.tonyodev.fetch2fileserver

/** Used to authenticate clients trying to connect to the Fetch File Server
 * instance this authenticator instance is attached to.*/
interface FetchFileServerAuthenticator {

    /** Method called when a client is attempting to connect to the Fetch File Server.
     * @param authorization the authorization token
     * @param fileRequest the fileRequest the client sent.
     * @return true if the authorize token is accepted. False otherwise.
     * */
    fun accept(authorization: String, fileRequest: FileRequest): Boolean

}