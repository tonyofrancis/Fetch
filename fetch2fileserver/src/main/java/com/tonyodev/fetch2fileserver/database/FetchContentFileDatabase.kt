package com.tonyodev.fetch2fileserver.database

import android.arch.persistence.room.*
import android.content.Context
import com.tonyodev.fetch2fileserver.ContentFile
import java.io.Closeable

class FetchContentFileDatabase(context: Context,
                               val databaseName: String) : Closeable {

    private val lock = Any()

    @Volatile
    private var closed = false

    val isClosed: Boolean
        get() = closed

    private val contentFileDatabase = Room.databaseBuilder(context,
            ContentFileDatabase::class.java, databaseName)
            .build()
    private val contentFileDao = contentFileDatabase.contentFileDao()

    fun insert(contentFile: ContentFile): Long {
        synchronized(lock) {
            throwExceptionIfClosed()
            return contentFileDao.insert(contentFile)
        }
    }

    fun insert(contentFileList: List<ContentFile>): List<Long> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return contentFileDao.insert(contentFileList)
        }
    }

    fun delete(contentFile: ContentFile) {
        synchronized(lock) {
            throwExceptionIfClosed()
            contentFileDao.delete(contentFile)
        }
    }

    fun delete(contentFileList: List<ContentFile>) {
        synchronized(lock) {
            throwExceptionIfClosed()
            contentFileDao.delete(contentFileList)
        }
    }

    fun deleteAll() {
        synchronized(lock) {
            throwExceptionIfClosed()
            contentFileDao.deleteAll()
        }
    }

    fun get(): List<ContentFile> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return contentFileDao.get()
        }
    }

    fun get(id: Long): ContentFile? {
        synchronized(lock) {
            throwExceptionIfClosed()
            return contentFileDao.get(id)
        }
    }

    fun get(ids: List<Long>): List<ContentFile> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return contentFileDao.get(ids)
        }
    }

    fun get(fileName: String): ContentFile? {
        synchronized(lock) {
            throwExceptionIfClosed()
            return contentFileDao.get(fileName)
        }
    }

    private fun getAll(page: Int, size: Int): List<ContentFile> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return if (page == -1 && size == -1) {
                contentFileDao.get()
            } else {
                var offset = 0
                for (i in 0 until page) {
                    offset += ContentFileDatabase.MAX_PAGE_SIZE
                }
                contentFileDao.getPage(size, offset)
            }
        }
    }

    fun getRequestedCatalog(page: Int, size: Int): String {
        synchronized(lock) {
            throwExceptionIfClosed()
            val contentFiles = getAll(page, size)
            val stringBuilder = StringBuilder("{\"catalog\":[")
            contentFiles.forEachIndexed { index, contentFile ->
                stringBuilder.append("{\"id\":${contentFile.id},\"name\":\"${contentFile.name}\"," +
                        "\"length\":${contentFile.length},\"customData\":\"${contentFile.customData}\"}")
                if (index != contentFiles.size - 1) {
                    stringBuilder.append(",")
                }
            }
            stringBuilder.append("],\"size\":${contentFiles.size}}")
            return stringBuilder.toString()
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
            contentFileDatabase.close()
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw Exception("FetchContentFileServerDatabase is closed")
        }
    }

}