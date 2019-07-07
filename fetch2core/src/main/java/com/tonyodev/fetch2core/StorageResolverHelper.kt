@file:JvmName("StorageResolverHelper")

package com.tonyodev.fetch2core

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import java.io.*

fun getOutputResourceWrapper(parcelFileDescriptor: ParcelFileDescriptor): OutputResourceWrapper {
    return getOutputResourceWrapper(parcelFileDescriptor.fileDescriptor, parcelFileDescriptor)
}

@JvmOverloads
fun getOutputResourceWrapper(fileDescriptor: FileDescriptor,
                             parcelFileDescriptor: ParcelFileDescriptor? = null): OutputResourceWrapper {
    return getOutputResourceWrapper(FileOutputStream(fileDescriptor), parcelFileDescriptor)
}

@JvmOverloads
fun getOutputResourceWrapper(fileOutputStream: FileOutputStream,
                             parcelFileDescriptor: ParcelFileDescriptor? = null): OutputResourceWrapper {
    return object : OutputResourceWrapper() {

        private val fileOutputStream = fileOutputStream
        private val parcelFileDescriptor = parcelFileDescriptor

        init {
            this.fileOutputStream.channel.position(0)
        }

        override fun write(byteArray: ByteArray, offSet: Int, length: Int) {
            this.fileOutputStream.write(byteArray, offSet, length)
        }

        override fun setWriteOffset(offset: Long) {
            this.fileOutputStream.channel.position(offset)
        }

        override fun flush() {
            this.fileOutputStream.flush()
        }

        override fun close() {
            this.fileOutputStream.close()
        }

    }
}

fun getOutputResourceWrapper(filePath: String, contentResolver: ContentResolver): OutputResourceWrapper {
    return if (isUriPath(filePath)) {
        getOutputResourceWrapper(Uri.parse(filePath), contentResolver)
    } else {
        getOutputResourceWrapper(File(filePath))
    }
}

fun getOutputResourceWrapper(fileUri: Uri, contentResolver: ContentResolver): OutputResourceWrapper {
    return when {
        fileUri.scheme == "content" -> {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(fileUri, "w")
            if (parcelFileDescriptor == null) {
                throw FileNotFoundException("$fileUri $FILE_NOT_FOUND")
            } else {
                getOutputResourceWrapper(parcelFileDescriptor)
            }
        }
        fileUri.scheme == "file" -> {
            val file = File(fileUri.path)
            if (file.exists() && file.canWrite()) {
                getOutputResourceWrapper(file)
            } else {
                val parcelFileDescriptor = contentResolver.openFileDescriptor(fileUri, "w")
                if (parcelFileDescriptor == null) {
                    throw FileNotFoundException("$fileUri $FILE_NOT_FOUND")
                } else {
                    getOutputResourceWrapper(parcelFileDescriptor)
                }
            }
        }
        else -> {
            throw FileNotFoundException("$fileUri $FILE_NOT_FOUND")
        }
    }
}

fun getOutputResourceWrapper(filePath: String): OutputResourceWrapper {
    val file = File(filePath)
    return if (file.exists()) getOutputResourceWrapper(file) else throw FileNotFoundException("$file $FILE_NOT_FOUND")
}

fun getOutputResourceWrapper(file: File): OutputResourceWrapper {
    if (!file.exists()) {
        throw FileNotFoundException("${file.canonicalPath} $FILE_NOT_FOUND")
    }
    return getOutputResourceWrapper(RandomAccessFile(file, "rw"))
}

fun getOutputResourceWrapper(randomAccessFile: RandomAccessFile): OutputResourceWrapper {
    return object : OutputResourceWrapper() {

        private val randomAccessFile = randomAccessFile

        init {
            this.randomAccessFile.seek(0)
        }

        override fun write(byteArray: ByteArray, offSet: Int, length: Int) {
            this.randomAccessFile.write(byteArray, offSet, length)
        }

        override fun setWriteOffset(offset: Long) {
            this.randomAccessFile.seek(offset)
        }

        override fun flush() {

        }

        override fun close() {
            this.randomAccessFile.close()
        }
    }
}

fun deleteFile(filePath: String, context: Context): Boolean {
    return if (isUriPath(filePath)) {
        val uri = Uri.parse(filePath)
        when {
            uri.scheme == "file" -> {
                val file = File(uri.path)
                if (file.canWrite() && file.exists()) deleteFile(file) else context.contentResolver.delete(uri, null, null) > 0
            }
            uri.scheme == "content" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                        && DocumentsContract.isDocumentUri(context, uri)) {
                    DocumentsContract.deleteDocument(context.contentResolver, uri)
                } else {
                    context.contentResolver.delete(uri, null, null) > 0
                }
            }
            else -> false
        }
    } else {
        deleteFile(File(filePath))
    }
}

fun renameFile(oldFile: String, newFile: String, context: Context): Boolean {
    return if (isUriPath(oldFile)) {
        val uri = Uri.parse(oldFile)
        when {
            uri.scheme == "file" -> {
                val file = File(uri.path)
                if (file.canWrite() && file.exists()) renameFile(file, File(newFile)) else {
                    val contentValue = ContentValues()
                    contentValue.put("uri", newFile)
                    context.contentResolver.update(uri, contentValue, null, null) > 0
                }
            }
            uri.scheme == "content" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        && DocumentsContract.isDocumentUri(context, uri)) {
                    DocumentsContract.renameDocument(context.contentResolver, uri, newFile) != null
                } else {
                    val contentValue = ContentValues()
                    contentValue.put("uri", newFile)
                    context.contentResolver.update(uri, contentValue, null, null) > 0
                }
            }
            else -> false
        }
    } else {
        renameFile(File(oldFile), File(newFile))
    }
}

fun createFileAtPath(filePath: String, increment: Boolean, context: Context): String {
    return if (isUriPath(filePath)) {
        val uri = Uri.parse(filePath)
        when {
            uri.scheme == "file" -> {
                createLocalFile(uri.path ?: filePath, increment)
            }
            uri.scheme == "content" -> {
                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "w")
                if (parcelFileDescriptor == null) {
                    throw IOException(FNC)
                } else {
                    filePath
                }
            }
            else -> throw IOException(FNC)
        }
    } else {
        createLocalFile(filePath, increment)
    }
}

fun createLocalFile(filePath: String, increment: Boolean): String {
    return if (!increment) {
        createFile(File(filePath))
        filePath
    } else {
        getIncrementedFileIfOriginalExists(filePath).absolutePath
    }
}

fun allocateFile(filePath: String, contentLength: Long, context: Context) {
    return if (isUriPath(filePath)) {
        val uri = Uri.parse(filePath)
        when {
            uri.scheme == "file" -> {
                allocateFile(File(uri.path ?: filePath), contentLength)
            }
            uri.scheme == "content" -> {
                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "w")
                if (parcelFileDescriptor == null) {
                    throw IOException(FILE_ALLOCATION_ERROR)
                } else {
                    allocateParcelFileDescriptor(parcelFileDescriptor, contentLength)
                }
            }
            else -> throw IOException(FILE_ALLOCATION_ERROR)
        }
    } else {
        allocateFile(File(filePath), contentLength)
    }
}

fun allocateParcelFileDescriptor(parcelFileDescriptor: ParcelFileDescriptor, contentLength: Long) {
    if (contentLength > 0) {
        try {
            val fileOutputStream = FileOutputStream(parcelFileDescriptor.fileDescriptor)
            if (fileOutputStream.channel.size() == contentLength) {
                return
            }
            fileOutputStream.channel.position(contentLength - 1.toLong())
            fileOutputStream.write(1)
        } catch (e: Exception) {
            throw IOException(FILE_ALLOCATION_ERROR)
        }
    }
}

fun allocateFile(file: File, contentLength: Long) {
    if (!file.exists()) {
        createFile(file)
    }
    if (file.length() == contentLength) {
        return
    }
    if (contentLength > 0) {
        try {
            val randomAccessFile = RandomAccessFile(file, "rw")
            randomAccessFile.setLength(contentLength)
        } catch (e: Exception) {
            throw IOException(FILE_ALLOCATION_ERROR)
        }
    }
}
