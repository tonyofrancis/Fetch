package com.tonyodev.fetch2core

interface FileServerDownloader : Downloader {

    /** Fetch a Catalog List of File Resources from a Fetch File Server
     * @param serverRequest the server request
     * @return list of File Resources
     * */
    fun getFetchFileServerCatalog(serverRequest: Downloader.ServerRequest): List<FileResource>

    /** Gets the File Request Type
     * @param serverRequest the server request
     * @return file request type.
     * */
    fun getFileRequestType(serverRequest: Downloader.ServerRequest): Int

}