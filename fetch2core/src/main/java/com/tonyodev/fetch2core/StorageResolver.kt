package com.tonyodev.fetch2core

/**
 * This interface can be implemented by a class to create a custom StorageResolver.
 * Fetch by default only works with the Java File object to save downloads.
 * Extending the Storage Resolver allows you to save Downloads using content providers
 * and other means of file storage.
 * */
interface StorageResolver {

    /** This method is called by Fetch to create the File object or create an entry in the storage database
     * whatever that is. This method can throw IOExceptions.
     * @param file the file
     * @param increment specify if the file already exist to create a new incremented file.
     * Example: text.txt, text(1).txt, text(2).txt.
     * @return returns the file (filePath, uri) if the file was created successfully. If null the file was not created.
     * */
    fun createFile(file: String, increment: Boolean = false): String

    /** This method is called by Fetch to delete the File object or remove an entry in the storage database
     * whatever that is. This method can throw IOExceptions.
     * @param file the file
     * @return returns true if the file was delete. Otherwise false.
     * */
    fun deleteFile(file: String): Boolean

    /**
     * This method is called by Fetch to request the OutputResourceWrapper that will be used to save
     * the download information too. If null is returned, Fetch will provide the OutputResourceWrapper.
     * This method is called on a background thread.
     * Note: If your request.file is a uri that points to a content provider you must override this method
     * and provide the proper OutputResourceWrapper object.
     * @param request The request information for the download.
     * @return OutputResourceWrapper object. Fetch will call the close method automatically
     *         after the disconnect(response) method is called. Fetch will provide a default
     *         OutputResourceWrapper. Override this method to provide your own OutputResourceWrapper.
     * */
    fun getRequestOutputResourceWrapper(request: Downloader.ServerRequest): OutputResourceWrapper

    /**
     * This method is called by Fetch for download requests that are downloading using the
     * FileDownloaderType.PARALLEL type. Fetch uses this directory to store the
     * temp files for the request. Fetch will select the default directory by default.
     * Override this method to provide your own temp directory.
     * Temp files in this directory are automatically
     * deleted by Fetch once a download completes or a request is removed.
     * This method is called on a background thread.
     * @param request the request information for the download.
     * @return the directory where the temp files will be stored
     * */
    fun getDirectoryForFileDownloaderTypeParallel(request: Downloader.ServerRequest): String

}