package com.tonyodev.fetch2core

import android.content.Context
import java.io.File
import java.io.RandomAccessFile

/** The default Storage Resolver used by Fetch. Extend this class if you want to provide your
 * own implementation.*/
open class DefaultStorageResolver(
        /* Context*/
        private val context: Context,
        /**The default temp directory used by Fetch.*/
        private val defaultTempDir: String) : StorageResolver {

    override fun createFile(file: String, increment: Boolean): String {
        return if (!isUriPath(file)) {
            if (!increment) {
                createFile(File(file))
                file
            } else {
                getIncrementedFileIfOriginalExists(file).absolutePath
            }
        } else {
            return ""
        }
    }

    override fun deleteFile(file: String): Boolean {
        return if (!isUriPath(file)) {
            try {
                val localFile = File(file)
                if (localFile.exists()) {
                    localFile.delete()
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        } else {
            //todo delete file from the content provider
            return false
        }
    }

    override fun getRequestOutputResourceWrapper(request: Downloader.ServerRequest): OutputResourceWrapper {
        return if (isUriPath(request.file)) {
            //TODO: HOW ARE WE GETTING THIS FROM THE CONTENT RESLOVER
            object : OutputResourceWrapper() {

                private val randomAccessFile = RandomAccessFile(File(request.file), "rw")

                init {
                    randomAccessFile.seek(0)
                }

                override fun write(byteArray: ByteArray, offSet: Int, length: Int) {
                    randomAccessFile.write(byteArray, offSet, length)
                }

                override fun setWriteOffset(offset: Long) {
                    randomAccessFile.seek(offset)
                }

                override fun flush() {

                }

                override fun close() {
                    randomAccessFile.close()
                }
            }
        } else {
            object : OutputResourceWrapper() {

                private val randomAccessFile = RandomAccessFile(File(request.file), "rw")

                init {
                    randomAccessFile.seek(0)
                }

                override fun write(byteArray: ByteArray, offSet: Int, length: Int) {
                    randomAccessFile.write(byteArray, offSet, length)
                }

                override fun setWriteOffset(offset: Long) {
                    randomAccessFile.seek(offset)
                }

                override fun flush() {

                }

                override fun close() {
                    randomAccessFile.close()
                }
            }
        }
    }

    override fun getDirectoryForFileDownloaderTypeParallel(request: Downloader.ServerRequest): String {
        return defaultTempDir
    }


}