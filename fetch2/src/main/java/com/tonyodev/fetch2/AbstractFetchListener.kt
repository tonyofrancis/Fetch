package com.tonyodev.fetch2

abstract class AbstractFetchListener : FetchListener {

    override fun onAttach(fetch: Fetch) {

    }

    override fun onDetach(fetch: Fetch) {

    }

    override fun onComplete(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long) {

    }

    override fun onError(id: Long, error: Error, progress: Int, downloadedBytes: Long, totalBytes: Long) {

    }

    override fun onProgress(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long) {

    }

    override fun onPause(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long) {

    }

    override fun onCancelled(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long) {

    }

    override fun onRemoved(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long) {

    }
}
