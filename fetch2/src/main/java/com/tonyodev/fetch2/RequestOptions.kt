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
    AUTO_REMOVE_ON_COMPLETED_DELETE_FILE,

    /** Fetch will auto replace an existing request where the id matches on enqueue.
     * If the old request has started the download process, this new request will continue where
     * it left off.*/
    REPLACE_ON_ENQUEUE,

    /** Fetch will auto replace an existing request where the id matches on enqueue.
     * If the old request has started the download process, this new request will force
     * downloading from the beginning.*/
    REPLACE_ON_ENQUEUE_FRESH,

    /** If a request with the same file path already exist in the Fetch database
     * when enqueuing a new request, Fetch will append a number to the file name.
     * Example: Original Path = "/data/dir/test.txt" to New Path = "/data/dir/test (1).txt".
     * The appended number will be auto incremented. Fetch will update the download file path
     * and may update the ID of the download if Fetch Generated a unique ID for the
     * initial request. After the successful enqueue, the success enqueue callback will be called
     * with the updated download information. Update any request and download references you have
     * with this new download information.*/
    ADD_AUTO_INCREMENT_TO_FILE_ON_ENQUEUE

}