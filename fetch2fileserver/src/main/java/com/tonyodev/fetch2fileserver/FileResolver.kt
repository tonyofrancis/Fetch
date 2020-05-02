package com.tonyodev.fetch2fileserver

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tonyodev.fetch2core.*
import com.tonyodev.fetch2core.server.FileRequest
import java.io.*

/**
 * Used to provide [InputResourceWrapper] for [FileResource]
 * */
abstract class FileResolver(context: Context) {

    protected val appContext: Context = context.applicationContext
    protected val contentResolver: ContentResolver = appContext.contentResolver

    /**
     * Returns the [InputResourceWrapper] for the catalog file
     * */
    open fun getCatalogInputWrapper(catalog: ByteArray, request: FileRequest, fileResource: FileResource): InputResourceWrapper {
        return object : InputResourceWrapper() {

            private val inputStream = ByteArrayInputStream(catalog, request.rangeStart.toInt(), fileResource.length.toInt())

            override fun read(byteArray: ByteArray, offSet: Int, length: Int): Int {
                return inputStream.read(byteArray, offSet, length)
            }

            override fun setReadOffset(offset: Long) {
                inputStream.skip(offset)
            }

            override fun close() {
                inputStream.close()
            }
        }
    }


    /**
     * Returns the [InputResourceWrapper] for a [FileResource]. Override this method if your [FileResource.file]
     * is not a traditional File but Uri, etc.
     * */
    @Throws(IOException::class)
    open fun getInputWrapper(fileResource: FileResource): InputResourceWrapper {
        val filePath = fileResource.file
        return if (isUriPath(filePath)) {
            getUriInputResourceWrapper(fileResource)
        } else {
            getFileInputResourceWrapper(fileResource)
        }
    }

    private fun getUriInputResourceWrapper(fileResource: FileResource): InputResourceWrapper {
        val fileUri = Uri.parse(fileResource.file)
        return when (fileUri.scheme) {
            "content" -> {
                val parcelFileDescriptor = contentResolver.openFileDescriptor(fileUri, "w")
                if (parcelFileDescriptor == null) {
                    throw FileNotFoundException("$fileUri $FILE_NOT_FOUND")
                } else {
                    createUriInputResourceWrapper(FileInputStream(parcelFileDescriptor.fileDescriptor), parcelFileDescriptor)
                }
            }
            "file" -> {
                val file = File(fileUri.path)
                if (file.exists() && file.canWrite()) {
                    createUriInputResourceWrapper(FileInputStream(file), null)
                } else {
                    val parcelFileDescriptor = contentResolver.openFileDescriptor(fileUri, "w")
                    if (parcelFileDescriptor == null) {
                        throw FileNotFoundException("$fileUri $FILE_NOT_FOUND")
                    } else {
                        createUriInputResourceWrapper(FileInputStream(parcelFileDescriptor.fileDescriptor), parcelFileDescriptor)
                    }
                }
            }
            else -> {
                throw FileNotFoundException("$fileUri $FILE_NOT_FOUND")
            }
        }
    }

    private fun createUriInputResourceWrapper(
            fileInputStream: FileInputStream,
            fileDescriptor: ParcelFileDescriptor? = null
    ): InputResourceWrapper {
        return object : InputResourceWrapper() {

            private val wrapperParcelFileDescriptor = fileDescriptor
            private val wrapperFileInputStream = fileInputStream

            init {
                wrapperFileInputStream.channel.position(0)
            }

            override fun read(byteArray: ByteArray, offSet: Int, length: Int): Int {
                return wrapperFileInputStream.read(byteArray, offSet, length)
            }

            override fun setReadOffset(offset: Long) {
                wrapperFileInputStream.channel.position(offset)
            }

            override fun close() {
                wrapperFileInputStream.close()
            }
        }
    }

    private fun getFileInputResourceWrapper(fileResource: FileResource): InputResourceWrapper {
        return object : InputResourceWrapper() {

            val randomAccessFile = RandomAccessFile(fileResource.file, "r")

            override fun read(byteArray: ByteArray, offSet: Int, length: Int): Int {
                return randomAccessFile.read(byteArray, offSet, length)
            }

            override fun setReadOffset(offset: Long) {
                randomAccessFile.seek(offset)
            }

            override fun close() {
                randomAccessFile.close()
            }
        }
    }

}