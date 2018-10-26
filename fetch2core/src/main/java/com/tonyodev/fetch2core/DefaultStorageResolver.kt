package com.tonyodev.fetch2core

import android.content.Context

/** The default Storage Resolver used by Fetch. Extend this class if you want to provide your
 * own implementation.*/
open class DefaultStorageResolver(
        /* Context*/
        protected val context: Context,
        /**The default temp directory used by Fetch.*/
        protected val defaultTempDir: String) : StorageResolver {

    override fun createFile(file: String, increment: Boolean): String {
        return createFileAtPath(file, increment, context)
    }

    override fun deleteFile(file: String): Boolean {
        return deleteFile(file, context)
    }

    override fun getRequestOutputResourceWrapper(request: Downloader.ServerRequest): OutputResourceWrapper {
        return getOutputResourceWrapper(request.file, context.contentResolver)
    }

    override fun getDirectoryForFileDownloaderTypeParallel(request: Downloader.ServerRequest): String {
        return defaultTempDir
    }

}