package com.tonyodev.fetch2

interface FetchListener : DownloadListener {
    fun onAttach(fetch: Fetch)
    fun onDetach(fetch: Fetch)
}
