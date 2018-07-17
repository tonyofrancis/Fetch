package com.tonyodev.fetch2rx.util

import com.tonyodev.fetch2rx.Convertible
import io.reactivex.Flowable

fun <T> Flowable<T>.toConvertible(): Convertible<T> {
    return Convertible(this)
}