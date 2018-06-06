package com.tonyodev.fetch2fileserver.database

import android.arch.persistence.room.*
import android.content.Context
import com.tonyodev.fetch2fileserver.FileResource
import java.io.Closeable

class FetchFileResourceDatabase(context: Context,
                                databaseName: String) : Closeable {

    private val lock = Any()

    @Volatile
    private var closed = false

    val isClosed: Boolean
        get() = closed

    private val fileResourceDatabase = Room.databaseBuilder(context,
            FileResourceInfoDatabase::class.java, databaseName)
            .build()
    private val fileResourceInfoDao = fileResourceDatabase.fileResourceInfoDao()

    fun insert(fileResource: FileResource): Long {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.insert(fileResource.toFileResourceInfo())
        }
    }

    fun insert(fileResourceList: List<FileResource>): List<Long> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.insert(fileResourceList.map { it.toFileResourceInfo() })
        }
    }

    fun delete(fileResource: FileResource) {
        synchronized(lock) {
            throwExceptionIfClosed()
            fileResourceInfoDao.delete(fileResource.toFileResourceInfo())
        }
    }

    fun delete(fileResourceList: List<FileResource>) {
        synchronized(lock) {
            throwExceptionIfClosed()
            fileResourceInfoDao.delete(fileResourceList.map { it.toFileResourceInfo() })
        }
    }

    fun deleteAll() {
        synchronized(lock) {
            throwExceptionIfClosed()
            fileResourceInfoDao.deleteAll()
        }
    }

    fun get(): List<FileResource> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.get().map { it.toFileResource() }
        }
    }

    fun get(id: Long): FileResource? {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.get(id)?.toFileResource()
        }
    }

    fun get(ids: List<Long>): List<FileResource> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.get(ids).map { it.toFileResource() }
        }
    }

    fun get(fileName: String): FileResource? {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.get(fileName)?.toFileResource()
        }
    }

    private fun getAll(page: Int, size: Int): List<FileResource> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return if (page == -1 && size == -1) {
                fileResourceInfoDao.get().map { it.toFileResource() }
            } else {
                var offset = 0
                for (i in 0 until page) {
                    offset += FileResourceInfoDatabase.MAX_PAGE_SIZE
                }
                fileResourceInfoDao.getPage(size, offset).map { it.toFileResource() }
            }
        }
    }

    fun getRequestedCatalog(page: Int, size: Int): String {
        synchronized(lock) {
            throwExceptionIfClosed()
            val fileResources = getAll(page, size)
            val stringBuilder = StringBuilder("{\"catalog\":[")
            fileResources.forEachIndexed { index, fileResource ->
                stringBuilder.append("{\"id\":${fileResource.id},\"name\":\"${fileResource.name}\"," +
                        "\"length\":${fileResource.length},\"customData\":\"${fileResource.customData}\"}")
                if (index != fileResources.size - 1) {
                    stringBuilder.append(",")
                }
            }
            stringBuilder.append("],\"size\":${fileResources.size}}")
            return stringBuilder.toString()
        }
    }

    override fun close() {
        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true
            fileResourceDatabase.close()
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw Exception("FetchFileResourceServerDatabase is closed")
        }
    }

}