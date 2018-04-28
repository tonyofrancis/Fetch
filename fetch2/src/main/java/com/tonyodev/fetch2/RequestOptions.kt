package com.tonyodev.fetch2

/**
 * Enumeration which contains the different Request Options that Fetch can use
 * when a situation occurs.
 * */
enum class RequestOptions {

    /** Fetch will auto remove the request from the database when it fails.
     * The file is not deleted.*/
    AUTO_REMOVE_ON_FAILED,

    /** Fetch will auto remove the request from the database when it fails.
     * The file is deleted if it exists.*/
    AUTO_REMOVE_ON_FAILED_DELETE_FILE,

    /** Fetch will auto remove the request from the database when it completes.
     * The file is not deleted*/
    AUTO_REMOVE_ON_COMPLETED,

    /** Fetch will auto remove the request from the database when it completes.
     * The file is deleted if it exists.*/
    AUTO_REMOVE_ON_COMPLETED_DELETE_FILE

}