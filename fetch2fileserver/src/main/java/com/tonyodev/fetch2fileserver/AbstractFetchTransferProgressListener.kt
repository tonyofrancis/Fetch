package com.tonyodev.fetch2fileserver

/** Listener that can be attached to a Fetch File Server instance
 * and reports connected clients content file transfer progress.*/
abstract class AbstractFetchTransferProgressListener : FetchTransferProgressListener {

    override fun onProgress(
            /* Connected client identifier*/
            client: String,
            /** Content file being transferred to client*/
            contentFile: ContentFile,
            /** Transfer progress*/
            progress: Int) {

    }

}