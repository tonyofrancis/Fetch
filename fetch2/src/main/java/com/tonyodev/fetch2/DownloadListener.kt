package com.tonyodev.fetch2

interface DownloadListener {
    fun onComplete(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long)
    fun onError(id: Long, error: Error, progress: Int, downloadedBytes: Long, totalBytes: Long)
    fun onProgress(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long)
    fun onPause(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long)
    fun onCancelled(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long)
    fun onRemoved(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long)
}
