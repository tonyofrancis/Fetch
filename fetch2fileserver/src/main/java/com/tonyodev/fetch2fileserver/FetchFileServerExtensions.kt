package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2core.FileResource
import com.tonyodev.fetch2core.Func
import com.tonyodev.fetch2core.Func2

/** Checks if a File Resource is managed by this instance.
 * @param fileResourceId file resource id
 * @param callback callback the result will be returned on. True if the file resource
 * is being managed. False otherwise.
 * */
fun FetchFileServer.containsFileResource(fileResourceId: Long, callback: (Boolean) -> Unit) {
    containsFileResource(fileResourceId, Func { result -> callback(result) })
}

/** Gets a list of all File Resources managed by this File Server instance.
 * @param callback callback the result is returned on.
 * */
fun FetchFileServer.getFileResources(callback: (List<FileResource>) -> Unit) {
    getFileResources(Func { result -> callback(result) })
}

/** Gets the Catalog(All File Resources) managed by this File Server instances
 * as JSON. The FileResources `file` field is excluded.
 * @param callback callback the result will be returned on.
 * */
fun FetchFileServer.getCatalog(callback: (String) -> Unit) {
    getCatalog(Func { result -> callback(result) })
}

/** Queries the File Server instance for a managed file resource if it exist.
 * @param fileResourceId file resource id
 * @param callback callback the result will be returned on. Result maybe null.
 * */
fun FetchFileServer.getFileResource(fileResourceId: Long, callback: (FileResource?) -> Unit) {
    getFileResource(fileResourceId, Func2 { result -> callback(result) })
}