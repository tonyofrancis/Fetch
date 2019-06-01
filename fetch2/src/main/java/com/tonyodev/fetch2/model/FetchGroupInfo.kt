package com.tonyodev.fetch2.model

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.FetchGroupObserver
import com.tonyodev.fetch2.Status
import com.tonyodev.fetch2.fetch.FetchModulesBuilder
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Reason
import com.tonyodev.fetch2core.calculateProgress

class FetchGroupInfo(override val id: Int = 0,
                     override val namespace: String): FetchGroup {

    private val observerSet = mutableSetOf<FetchGroupObserver>()

    @Volatile
    override var downloads: List<Download> = emptyList()
        set(value) {
            field = value
            queuedDownloads = value.filter { it.status == Status.QUEUED }
            addedDownloads = value.filter { it.status == Status.ADDED }
            pausedDownloads = value.filter { it.status == Status.PAUSED }
            downloadingDownloads = value.filter { it.status == Status.DOWNLOADING }
            completedDownloads = value.filter { it.status == Status.COMPLETED }
            cancelledDownloads = value.filter { it.status == Status.CANCELLED }
            failedDownloads = value.filter { it.status == Status.FAILED }
            deletedDownloads = value.filter { it.status == Status.DELETED }
            removedDownloads = value.filter { it.status == Status.REMOVED }
        }

    fun update(downloads: List<Download>, triggerDownload: Download?, reason: Reason) {
        this.downloads = downloads
        if (reason != Reason.DOWNLOAD_BLOCK_UPDATED) {
            FetchModulesBuilder.mainUIHandler.post {
                synchronized(observerSet) {
                    observerSet.iterator().forEach {
                        it.onChanged(downloads, reason)
                        if (triggerDownload != null) {
                            it.onChanged(downloads, triggerDownload, reason)
                        }
                    }
                }
            }
        }
    }

    override var queuedDownloads: List<Download> = emptyList()

    override var addedDownloads: List<Download> = emptyList()

    override var pausedDownloads: List<Download> = emptyList()

    override var downloadingDownloads: List<Download> = emptyList()

    override var completedDownloads: List<Download> = emptyList()

    override var cancelledDownloads: List<Download> = emptyList()

    override var failedDownloads: List<Download> = emptyList()

    override var deletedDownloads: List<Download> = emptyList()

    override var removedDownloads: List<Download> = emptyList()

    override val groupDownloadProgress: Int
        get() {
            return if (downloads.any { it.total < 1L }) {
                -1
            } else {
                var downloaded = 0L
                var total = 0L
                for (download in downloads) {
                    downloaded += download.downloaded
                    total += download.total
                }
                return calculateProgress(downloaded, total)
            }
        }

    override val observers: Set<FetchObserver<List<Download>>>
        get() {
            return synchronized(observerSet) {
                observerSet.toMutableSet()
            }
        }

    override fun addFetchGroupObservers(vararg fetchGroupObservers: FetchGroupObserver) {
        synchronized(observerSet) {
            val newFetchGroupObservers = fetchGroupObservers.distinct()
            val addedObservers = mutableListOf<FetchGroupObserver>()
            for (fetchGroupObserver in newFetchGroupObservers) {
                if (!observerSet.contains(fetchGroupObserver)) {
                    observerSet.add(fetchGroupObserver)
                    addedObservers.add(fetchGroupObserver)
                }
            }
            FetchModulesBuilder.mainUIHandler.post {
                for (addedObserver in addedObservers) {
                    addedObserver.onChanged(downloads, Reason.OBSERVER_ATTACHED)
                }
            }
        }
    }

    override fun removeFetchGroupObservers(vararg fetchGroupObservers: FetchGroupObserver) {
        synchronized(observerSet) {
            for (fetchGroupObserver in fetchGroupObservers) {
                observerSet.remove(fetchGroupObserver)
            }
        }
    }

}