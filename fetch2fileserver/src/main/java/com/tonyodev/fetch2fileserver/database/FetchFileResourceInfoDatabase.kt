package com.tonyodev.fetch2fileserver.database

import android.arch.persistence.room.*
import android.content.Context
import java.io.Closeable

class FetchFileResourceInfoDatabase(context: Context,
                                    databaseName: String) : Closeable {

    private val lock = Any()

    @Volatile
    private var closed = false

    val isClosed: Boolean
        get() {
            return closed
        }

    private val fileResourceInfoDatabase = Room.databaseBuilder(context,
            FileResourceInfoDatabase::class.java, databaseName)
            .build()
    private val fileResourceInfoDao = fileResourceInfoDatabase.fileResourceInfoDao()

    fun insert(fileResourceInfo: FileResourceInfo): Long {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.insert(fileResourceInfo)
        }
    }

    fun insert(fileResourceInfoList: List<FileResourceInfo>): List<Long> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.insert(fileResourceInfoList)
        }
    }

    fun delete(fileResourceInfo: FileResourceInfo) {
        synchronized(lock) {
            throwExceptionIfClosed()
            fileResourceInfoDao.delete(fileResourceInfo)
        }
    }

    fun delete(fileResourceInfoList: List<FileResourceInfo>) {
        synchronized(lock) {
            throwExceptionIfClosed()
            fileResourceInfoDao.delete(fileResourceInfoList)
        }
    }

    fun deleteAll() {
        synchronized(lock) {
            throwExceptionIfClosed()
            fileResourceInfoDao.deleteAll()
        }
    }

    fun get(): List<FileResourceInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.get()
        }
    }

    fun get(id: Long): FileResourceInfo? {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.get(id)
        }
    }

    fun get(ids: List<Long>): List<FileResourceInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.get(ids)
        }
    }

    fun get(fileName: String): FileResourceInfo? {
        synchronized(lock) {
            throwExceptionIfClosed()
            return fileResourceInfoDao.get(fileName)
        }
    }

    private fun getAll(page: Int, size: Int): List<FileResourceInfo> {
        synchronized(lock) {
            throwExceptionIfClosed()
            return if (page == -1 && size == -1) {
                fileResourceInfoDao.get()
            } else {
                var offset = 0
                for (i in 0 until page) {
                    offset += FileResourceInfoDatabase.MAX_PAGE_SIZE
                }
                fileResourceInfoDao.getPage(size, offset)
            }
        }
    }

    fun getRequestedCatalog(page: Int = -1, size: Int = -1): String {
        synchronized(lock) {
            throwExceptionIfClosed()
            val fileResources = getAll(page, size)
            val stringBuilder = StringBuilder("{\"catalog\":[")
            fileResources.forEachIndexed { index, fileResource ->
                stringBuilder.append("{\"id\":${fileResource.id},\"name\":\"${fileResource.name}\"," +
                        "\"length\":${fileResource.length},\"extras\":${fileResource.extras}," +
                        "\"md5\":\"${fileResource.md5}\"}")
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
            fileResourceInfoDatabase.close()
        }
    }

    private fun throwExceptionIfClosed() {
        if (closed) {
            throw Exception("FetchFileResourceInfoServerDatabase is closed")
        }
    }

}