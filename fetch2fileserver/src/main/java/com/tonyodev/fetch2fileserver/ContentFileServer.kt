package com.tonyodev.fetch2fileserver

interface ContentFileServer {

    val id: String
    val port: Int
    val address: String
    val isShutDown: Boolean

    fun start()
    fun shutDown(forced: Boolean = false)
    fun setAuthenticator(contentFileServerAuthenticator: ContentFileServerAuthenticator?)
    fun setDelegate(contentFileServerDelegate: ContentFileServerDelegate?)
    fun setProgressListener(contentFileProgressListener: ContentFileProgressListener?)
    fun addContentFile(contentFile: ContentFile)
    fun addContentFiles(contentFiles: Collection<ContentFile>)
    fun removeContentFile(contentFile: ContentFile)
    fun removeContentFiles(contentFiles: Collection<ContentFile>)
    fun removeAllContentFiles()
    fun containsContentFile(contentId: Int, callback: (Boolean) -> Unit)
    fun getContentFiles(callback: (List<ContentFile>) -> Unit)
    fun getFullCatalog(callback: (String) -> Unit)
    fun getContentFile(contentId: Int, callback: (ContentFile?) -> Unit)

}