package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2core.FileResource

/** Listener that can be attached to a Fetch File Server instance
 * and reports connected clients content file transfer progress.*/
interface FetchTransferProgressListener {

    /**
     * Method called to report the progress of a file resource transfer to a client.
     * @param client Connected client identifier
     * @param fileResource file resource being transferred to client
     * @param progress Transfer progress
     * */
    fun onProgress(client: String, fileResource: FileResource, progress: Int)

}