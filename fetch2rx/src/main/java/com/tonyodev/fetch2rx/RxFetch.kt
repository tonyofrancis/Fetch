package com.tonyodev.fetch2rx

import android.content.Context
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2.exception.FetchException
import com.tonyodev.fetch2.fetch.FetchBuilder
import com.tonyodev.fetch2.fetch.FetchModulesBuilder

/**
 * A light weight file download manager for Android with Rx features.
 * Features: Background downloading,
 *           Queue based Priority downloading,
 *           Pause & Resume downloads,
 *           Network specific downloading and more...
 * */
interface RxFetch : Fetch {

    /**
     * Queues a request for downloading. If Fetch fails to enqueue the request,
     * the returned convertible object when converted to an observable or flowable and subscribed to,
     * will throw an exception indicating the error.
     * Errors that may cause Fetch to fail the enqueue are :
     * 1. No storage space on the device.
     * 2. Fetch is already managing the same request. This means that a request with the same url
     * and file name is already managed.
     * It would be best to check if Fetch is managing a download before enqueuing.
     * @param request Download Request
     * @throws FetchException if this instance of Fetch has been closed.
     * @return A Convertible object that allows you to get the results as on observable or
     * flowable
     * */
    fun enqueue(request: Request): Convertible<Download>

    /**
     * Queues a request list for downloading. If Fetch fails to enqueue a
     * download request because an error occurred, all other request in the list will fail and the
     * the returned convertible object when converted to an observable or flowable and subscribed to,
     * will throw an exception indicating the error.
     * Errors that may cause Fetch to fail the enqueue are :
     * 1. No storage space on the device.
     * 2. Fetch is already managing the same request. This means that a request with the same url
     * and file name is already managed.
     * It would be best to check if Fetch is managing a download before enqueuing.
     * @param requests Download Request
     * @throws FetchException if this instance of Fetch has been closed.
     * @return A Convertible object that allows you to get the results as on observable or
     * flowable
     * */
    fun enqueue(requests: List<Request>): Convertible<List<Download>>

    /** Updates and replaces an existing download's groupId, headers, priority and network type information.
     * If the download does not exist and the returned convertible object when converted
     * to an observable or flowable and subscribed to, will throw an exception indicating the
     * error.
     * @see com.tonyodev.fetch2.RequestInfo for more details.
     * @param id Id of existing download
     * @param requestInfo Request Info object
     * @throws FetchException if this instance of Fetch has been closed.
     * @return A Convertible object that allows you to get the results as on observable or
     * flowable*/
    fun updateRequest(id: Int, requestInfo: RequestInfo): Convertible<Download>

    /**
     * Gets all downloads managed by this instance of Fetch.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return A Convertible object that allows you to get the results as on observable or
     * flowable*/
    fun getDownloads(): Convertible<List<Download>>

    /**
     * Gets the downloads which match an id in the list. Only successful matches will be returned.
     * @param idList Id list to perform id query against.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return A Convertible object that allows you to get the results as on observable or
     * flowable*/
    fun getDownloads(idList: List<Int>): Convertible<List<Download>>

    /**
     * Gets the download which has the specified id. If the download
     * does not exist the returned convertible object when converted
     * to an observable or flowable and subscribed to, will throw an exception indicating the
     * error.
     * @param id Download id
     * @throws FetchException if this instance of Fetch has been closed.
     * @return A Convertible object that allows you to get the results as on observable or
     * flowable*/
    fun getDownload(id: Int): Convertible<Download>

    /**
     * Gets all downloads in the specified group.
     * @param groupId group id to query.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return A Convertible object that allows you to get the results as on observable or
     * flowable*/
    fun getDownloadsInGroup(groupId: Int): Convertible<List<Download>>

    /**
     * Gets all downloads with a specific status.
     * @see com.tonyodev.fetch2.Status
     * @param status Status to query.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return A Convertible object that allows you to get the results as on observable or
     * flowable*/
    fun getDownloadsWithStatus(status: Status): Convertible<List<Download>>

    /**
     * Gets all downloads in a specific group with a specific status.
     * @see com.tonyodev.fetch2.Status
     * @param groupId group id to query.
     * @param status Status to query.
     * @throws FetchException if this instance of Fetch has been closed.
     * @return A Convertible object that allows you to get the results as on observable or
     * flowable*/
    fun getDownloadsInGroupWithStatus(groupId: Int, status: Status): Convertible<List<Download>>

    /**
     * Builder used to configure and create instances of rxFetch with rxJava goodies.
     * */
    class Builder constructor(
            /** Context*/
            context: Context,

            /** The namespace which Fetch operates in. Fetch uses
             * a namespace to create a database that the instance will use. Downloads
             * enqueued on the instance will belong to the namespace and will not be accessible
             * from any other namespace. An App can only have one Active Fetch instance with the
             * specified namespace. * In essence an App can have many instances of fetch
             * with a different namespaces.
             * */
            namespace: String)
        : FetchBuilder<Builder, RxFetch>(context, namespace) {

        /** Builds a new instance of RxFetch with the proper configuration.
         * @throws FetchException if an active instance of Fetch with the same namespace already
         * exists.
         * @return New instance of RxFetch.
         * */
        override fun build(): RxFetch {
            val modules = FetchModulesBuilder.buildModulesFromPrefs(getBuilderPrefs())
            return RxFetchImpl.newInstance(modules)
        }
    }

}