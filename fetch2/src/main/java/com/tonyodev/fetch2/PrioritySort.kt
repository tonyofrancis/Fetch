package com.tonyodev.fetch2

/**
 * Used to dictate the order in which Fetch processes request/downloads
 * based on time created.
 * */
enum class PrioritySort {

    /** Ascending Order. Downloads that were created the earliest are processed.*/
    ASC,

    /**
     * Descending Order. Downloads that were created lasted are processed.
     * */
    DESC

}