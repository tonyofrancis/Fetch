package com.tonyodev.fetch2fileserver

interface FetchTransferProgressListener {

    fun onProgress(client: String, contentFile: ContentFile, progress: Int)

}