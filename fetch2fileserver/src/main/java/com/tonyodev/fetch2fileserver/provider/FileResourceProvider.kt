package com.tonyodev.fetch2fileserver.provider

import com.tonyodev.fetch2core.FileResource

interface FileResourceProvider {

    val id: String

    fun execute()

    fun interrupt()

    fun isServingFileResource(fileResource: FileResource): Boolean

}