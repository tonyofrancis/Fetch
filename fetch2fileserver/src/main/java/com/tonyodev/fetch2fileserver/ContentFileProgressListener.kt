package com.tonyodev.fetch2fileserver

interface ContentFileProgressListener {

    fun onProgress(client: String, contentFile: ContentFile, progress: Int)

}