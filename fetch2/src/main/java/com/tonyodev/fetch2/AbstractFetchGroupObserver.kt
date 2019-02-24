package com.tonyodev.fetch2

/**
 * Abstract implementation of FetchGroupObserver
 * */
abstract class AbstractFetchGroupObserver: FetchGroupObserver {

    /**
     * Method called when the download list has changed.
     * @param data the download list.
     * @param triggerDownload the download that triggered the change.
     * */
    override fun onChanged(data: List<Download>, triggerDownload: Download) {

    }

    /**
     * Method called when the data on the observing object has changed.
     * @param data the data.
     * */
    override fun onChanged(data: List<Download>) {

    }

}