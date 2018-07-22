package com.tonyodev.fetch2fileserver.database

import com.tonyodev.fetch2core.FileResource
import org.json.JSONObject

fun FileResource.toFileResourceInfo(): FileResourceInfo {
    val fileResourceInfo = FileResourceInfo()
    fileResourceInfo.id = id
    fileResourceInfo.file = file
    fileResourceInfo.length = length
    fileResourceInfo.name = name
    fileResourceInfo.md5 = md5
    fileResourceInfo.customData = if (customData.isEmpty()) {
        "{}"
    } else {
        val json = JSONObject()
        customData.iterator().forEach {
            json.put(it.key, it.value)
        }
        json.toString()
    }
    return fileResourceInfo
}

fun FileResourceInfo.toFileResource(): FileResource {
    val fileResource = FileResource()
    fileResource.id = id
    fileResource.file = file
    fileResource.length = length
    fileResource.name = name
    fileResource.md5 = md5
    fileResource.customData = try {
        val map = mutableMapOf<String, String>()
        val json = JSONObject(customData)
        json.keys().forEach {
            map[it] = json.getString(it)
        }
        map
    } catch (e: Exception) {
        mutableMapOf()
    }
    return fileResource
}