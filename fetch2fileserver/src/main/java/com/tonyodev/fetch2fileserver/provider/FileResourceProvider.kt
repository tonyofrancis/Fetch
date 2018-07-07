package com.tonyodev.fetch2fileserver.provider

import com.tonyodev.fetch2core.FileResource

interface FileResourceProvider {

    fun execute()

    fun interrupt()

    fun isServingFileResource(fileResource: FileResource): Boolean

}