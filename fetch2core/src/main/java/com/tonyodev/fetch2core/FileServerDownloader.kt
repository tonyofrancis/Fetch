package com.tonyodev.fetch2core

interface FileServerDownloader : Downloader {

    /** Fetch a Catalog List of File Resources from a Fetch File Server
     * @param serverRequest the server request
     * @return list of File Resources
     * */
    fun getFetchFileServerCatalog(serverRequest: Downloader.ServerRequest): List<FileResource>

}