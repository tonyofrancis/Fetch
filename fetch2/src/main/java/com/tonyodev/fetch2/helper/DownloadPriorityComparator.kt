package com.tonyodev.fetch2.helper

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.util.defaultPriority

open class DownloadPriorityComparator : Comparator<Download> {

    override fun compare(firstDownload: Download?, secondDownload: Download?): Int {
        val firstDownloadPriority = firstDownload?.priority?.value ?: defaultPriority.value
        val secondDownloadPriority = secondDownload?.priority?.value ?: defaultPriority.value
        return when {
            firstDownloadPriority > secondDownloadPriority -> 1
            firstDownloadPriority == secondDownloadPriority -> {
                val firstDownloadCreated = firstDownload?.created ?: 0
                val secondDownloadCreated = secondDownload?.created ?: 0
                when {
                    firstDownloadCreated > secondDownloadCreated -> 1
                    firstDownloadCreated == secondDownloadCreated -> 0
                    else -> -1
                }
            }
            else -> -1
        }
    }

}