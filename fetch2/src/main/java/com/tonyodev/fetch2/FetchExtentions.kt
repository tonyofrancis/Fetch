package com.tonyodev.fetch2

import com.tonyodev.fetch2.exception.FetchException


/**
 * Queues a request for downloading. If Fetch fails to enqueue the request,
 * func2 will be called with the error message.
 * Errors that may cause Fetch to fail the enqueue are :
 * 1. No storage space on the device.
 * 2. Fetch is already managing the same request. This means that a request with the same url
 * and file name is already managed.
 * @param request Download Request
 * @param func Callback that the enqueued request will be returned on.
 *             Fetch may update a request depending on the initial request's Enqueue Action.
 *             Update old request references with this request.
 * @param func2 Callback that is called when enqueuing a request fails. An error is returned.
 * @throws FetchException if this instance of Fetch has been closed.
 * @return Instance
 * */
fun Fetch.enqueue(request: Request, func: ((Request) -> Unit)?, func2: ((Error) -> Unit)?): Fetch {
    return enqueue(request, object : Func<Request> {
        override fun call(t: Request) {
            func?.invoke(t)
        }
    }, object : Func<Error> {
        override fun call(t: Error) {
            func2?.invoke(t)
        }
    })
}

/**
 * Queues a list of requests for downloading. If Fetch fails to enqueue a
 * download request because an error occurred, all other request in the list will
 * fail. Func2 will be called with the error message.
 * Errors that may cause Fetch to fail the enqueue are :
 * 1. No storage space on the device.
 * 2. Fetch is already managing the same request. This means that a request with the same url
 * and file name is already managed.
 * @param requests Request List
 * @param func Callback that the enqueued request will be returned on.
 *             Fetch may update a request depending on the initial request's Enqueue Action.
 *             Update old request references with this request.
 * @param func2 Callback that is called when enqueuing a request fails. An error is returned.
 * @throws FetchException if this instance of Fetch has been closed.
 * @return Instance
 * */
fun Fetch.enqueue(requests: List<Request>, func: ((List<Request>) -> Unit)?, func2: ((Error) -> Unit)?): Fetch {
    return enqueue(requests, object : Func<List<Request>> {
        override fun call(t: List<Request>) {
            func?.invoke(t)
        }
    }, object : Func<Error> {
        override fun call(t: Error) {
            func2?.invoke(t)
        }
    })
}

/** Updates and replaces an existing download's groupId, headers, priority and network
 * type information.
 * @see com.tonyodev.fetch2.RequestInfo for more details.
 * @param id Id of existing download
 * @param requestInfo Request Info object
 * @param func Successful callback that the download will be returned on.
 * @param func2 Failed callback that the error will be returned on.
 * @throws FetchException if this instance of Fetch has been closed.
 * @return Instance
 * */
fun Fetch.updateRequest(id: Int, requestInfo: RequestInfo, func: ((Download) -> Unit)?,
                        func2: ((Error) -> Unit)?): Fetch {
    return updateRequest(id, requestInfo, object : Func<Download> {
        override fun call(t: Download) {
            func?.invoke(t)
        }
    }, object : Func<Error> {
        override fun call(t: Error) {
            func2?.invoke(t)
        }
    })
}

/**
 * Gets all downloads managed by this instance of Fetch.
 * @param func Callback that the results will be returned on.
 * @throws FetchException if this instance of Fetch has been closed.
 * @return Instance
 * */
fun Fetch.getDownloads(func: (List<Download>) -> Unit): Fetch {
    return getDownloads(object : Func<List<Download>> {
        override fun call(t: List<Download>) {
            func(t)
        }
    })
}

/**
 * Gets the downloads which match an id in the list. Only successful matches will be returned.
 * @param idList Id list to perform id query against.
 * @param func Callback that the results will be returned on.
 * @throws FetchException if this instance of Fetch has been closed.
 * @return Instance
 * */
fun Fetch.getDownloads(idList: List<Int>, func: (List<Download>) -> Unit): Fetch {
    return getDownloads(idList, object : Func<List<Download>> {
        override fun call(t: List<Download>) {
            func(t)
        }
    })
}

/**
 * Gets the download which has the specified id. If the download
 * does not exist null will be returned.
 * @param id Download id
 * @param func Callback that the results will be returned on. Result maybe null.
 * @throws FetchException if this instance of Fetch has been closed.
 * @return Instance
 * */
fun Fetch.getDownload(id: Int, func: (Download?) -> Unit): Fetch {
    return getDownload(id, object : Func2<Download?> {
        override fun call(t: Download?) {
            func(t)
        }
    })
}

/**
 * Gets all downloads in the specified group.
 * @param groupId group id to query.
 * @param func Callback that the results will be returned on.
 * @throws FetchException if this instance of Fetch has been closed.
 * @return Instance
 * */
fun Fetch.getDownloadsInGroup(groupId: Int, func: (List<Download>) -> Unit): Fetch {
    return getDownloadsInGroup(groupId, object : Func<List<Download>> {
        override fun call(t: List<Download>) {
            func(t)
        }
    })
}

/**
 * Gets all downloads with a specific status.
 * @see com.tonyodev.fetch2.Status
 * @param status Status to query.
 * @param func Callback that the results will be returned on.
 * @throws FetchException if this instance of Fetch has been closed.
 * @return Instance
 * */
fun Fetch.getDownloadsWithStatus(status: Status, func: (List<Download>) -> Unit): Fetch {
    return getDownloadsWithStatus(status, object : Func<List<Download>> {
        override fun call(t: List<Download>) {
            func(t)
        }
    })
}

/**
 * Gets all downloads in a specific group with a specific status.
 * @see com.tonyodev.fetch2.Status
 * @param groupId group id to query.
 * @param status Status to query.
 * @param func Callback that the results will be returned on.
 * @throws FetchException if this instance of Fetch has been closed.
 * @return Instance
 * */
fun Fetch.getDownloadsInGroupWithStatus(groupId: Int, status: Status, func: (List<Download>) -> Unit): Fetch {
    return getDownloadsInGroupWithStatus(groupId, status, object : Func<List<Download>> {
        override fun call(t: List<Download>) {
            func(t)
        }
    })
}