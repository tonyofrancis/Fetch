package com.tonyodev.fetch2fileserver

import com.tonyodev.fetch2.Func

/** Checks if a File Resource is managed by this instance.
 * @param fileResourceId file resource id
 * @param callback callback the result will be returned on. True if the file resource
 * is being managed. False otherwise.
 * */
fun FetchFileServer.containsFileResource(fileResourceId: Long, callback: (Boolean) -> Unit) {
    containsFileResource(fileResourceId, object : Func<Boolean> {
        override fun call(t: Boolean) {
            callback(t)
        }
    })
}

/** Gets a list of all File Resources managed by this File Server instance.
 * @param callback callback the result is returned on.
 * */
fun FetchFileServer.getFileResources(callback: (List<FileResource>) -> Unit) {
    getFileResources(object : Func<List<FileResource>> {
        override fun call(t: List<FileResource>) {
            callback(t)
        }
    })
}

/** Gets the Catalog(All File Resources) managed by this File Server instances
 * as JSON. The FileResources `file` field is excluded.
 * @param callback callback the result will be returned on.
 * */
fun FetchFileServer.getCatalog(callback: (String) -> Unit) {
    getCatalog(object : Func<String> {
        override fun call(t: String) {
            callback(t)
        }
    })
}

/** Queries the File Server instance for a managed file resource if it exist.
 * @param fileResourceId file resource id
 * @param callback callback the result will be returned on. Result maybe null.
 * */
fun FetchFileServer.getFileResource(fileResourceId: Long, callback: (FileResource?) -> Unit) {
    getFileResource(fileResourceId, object : Func<FileResource?> {
        override fun call(t: FileResource?) {
            callback(t)
        }
    })
}