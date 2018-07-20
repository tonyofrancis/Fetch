package com.tonyodev.fetch2.downloader

class DownloadManagerCoordinator(val namespace: String) {

    private val lock = Any()
    private val fileDownloaderMap = mutableMapOf<Int, FileDownloader?>()

    fun interruptDownload(downloadId: Int) {
        synchronized(lock) {
            val fileDownloader = fileDownloaderMap[downloadId]
            if (fileDownloader != null) {
                fileDownloader.interrupted = true
                fileDownloaderMap.remove(downloadId)
            }
        }
    }

    fun addFileDownloader(downloadId: Int, fileDownloader: FileDownloader?) {
        synchronized(lock) {
            fileDownloaderMap[downloadId] = fileDownloader
        }
    }

    fun removeFileDownloader(downloadId: Int) {
        synchronized(lock) {
            fileDownloaderMap.remove(downloadId)
        }
    }

    fun getFileDownloaderList(): List<FileDownloader?> {
        return synchronized(lock) {
            fileDownloaderMap.values.toList()
        }
    }

    fun containsFileDownloader(downloadId: Int): Boolean {
        return synchronized(lock) {
            fileDownloaderMap.containsKey(downloadId)
        }
    }

    fun clearAll() {
        synchronized(lock) {
            fileDownloaderMap.clear()
        }
    }

}