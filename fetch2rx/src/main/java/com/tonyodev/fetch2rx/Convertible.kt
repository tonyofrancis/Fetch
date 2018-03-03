package com.tonyodev.fetch2rx

import io.reactivex.Flowable
import io.reactivex.Observable

/**
 * This class is used as a converter class to access
 * results returned by Fetch as an observable or flowable object.
 * */
class Convertible<T>(private val data: Flowable<T>) {

    /** Access the results returned by Fetch as a Flowable.*/
    val flowable: Flowable<T>
        @JvmName("asFlowable")
        get() = data

    /** Access results returned by Fetch as an observable.*/
    val observable: Observable<T>
        @JvmName("asObservable")
        get() = data.toObservable()
}