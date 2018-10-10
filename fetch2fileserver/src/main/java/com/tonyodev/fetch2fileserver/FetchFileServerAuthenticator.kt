package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2core.server.FileRequest

/** Used to authenticate clients trying to connect to the Fetch File Server
 * instance this authenticator instance is attached to.*/
interface FetchFileServerAuthenticator {

    /** Method called when a client is attempting to connect to the Fetch File Server.
     * This method is called on a background thread.
     * @param sessionId sessionId
     * @param authorization the authorization token
     * @param fileRequest the fileRequest the client sent.
     * @return true if the authorize token is accepted. False otherwise.
     * */
    fun accept(sessionId: String, authorization: String, fileRequest: FileRequest): Boolean

}