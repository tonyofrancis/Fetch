package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2core.FileResource
import com.tonyodev.fetch2core.server.FileRequest

/** Listener that can be attached to a Fetch File Server instance
 * and reports connected clients content file transfer progress.*/
interface FetchTransferListener {

    /**
     * Method called to report the start of a file resource transfer to a client.
     * @param sessionId SessionId
     * @param fileRequest File Request
     * @param fileResource file resource being transferred to client
     * */
    fun onStarted(sessionId: String, fileRequest: FileRequest, fileResource: FileResource)

    /**
     * Method called to report the progress of a file resource transfer to a client.
     * @param sessionId SessionId
     * @param fileRequest File Request
     * @param fileResource file resource being transferred to client
     * @param progress Transfer progress
     * */
    fun onProgress(sessionId: String, fileRequest: FileRequest, fileResource: FileResource, progress: Int)

    /**
     * Method called to report the completed file resource transfer to a client.
     * @param sessionId SessionId
     * @param fileRequest File Request
     * @param fileResource file resource being transferred to client
     * */
    fun onComplete(sessionId: String, fileRequest: FileRequest, fileResource: FileResource)

    /**
     * Method called to report an error that occurred whiles transferring a file resource to client.
     * @param sessionId SessionId
     * @param fileRequest File Request
     * @param fileResource file resource being transferred to client
     * @param throwable error
     * */
    fun onError(sessionId: String, fileRequest: FileRequest, fileResource: FileResource, throwable: Throwable)

}