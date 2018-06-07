package com.tonyodev.fetch2fileserver.database

import com.tonyodev.fetch2fileserver.FileResource

fun FileResource.toFileResourceInfo(): FileResourceInfo {
    val fileResourceInfo = FileResourceInfo()
    fileResourceInfo.id = id
    fileResourceInfo.customData = customData
    fileResourceInfo.file = file
    fileResourceInfo.length = length
    fileResourceInfo.name = name
    fileResourceInfo.md5 = md5
    return fileResourceInfo
}

fun FileResourceInfo.toFileResource(): FileResource {
    val fileResource = FileResource()
    fileResource.id = id
    fileResource.customData = customData
    fileResource.file = file
    fileResource.length = length
    fileResource.name = name
    fileResource.md5 = md5
    return fileResource
}