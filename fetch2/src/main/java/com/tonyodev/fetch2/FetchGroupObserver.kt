package com.tonyodev.fetch2

import com.tonyodev.fetch2core.FetchObserver

/**
 * Fetch observer for groups. This observer also specifies which download
 * triggers the onChanged in the group.
 * */
interface FetchGroupObserver: FetchObserver<List<Download>> {

    /**
     * Method called when the download list has changed.
     * @param data the download list.
     * @param triggerDownload the download that triggered the change.
     * */
    fun onChanged(data: List<Download>, triggerDownload: Download)

}