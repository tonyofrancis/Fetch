package com.tonyodev.fetch2

import com.tonyodev.fetch2core.Reason

/**
 * Abstract implementation of FetchGroupObserver
 * */
abstract class AbstractFetchGroupObserver: FetchGroupObserver {

    /**
     * Method called when the download list has changed.
     * @param data the download list.
     * @param triggerDownload the download that triggered the change.
     * @param reason the reason why onChanged was called for the triggered download.
     * */
    override fun onChanged(data: List<Download>, triggerDownload: Download, reason: Reason) {

    }

    /**
     * Method called when the data on the observing object has changed.
     * @param data the data.
     * @param reason the reason why the onChanged method was called.
     * */
    override fun onChanged(data: List<Download>, reason: Reason) {

    }

}