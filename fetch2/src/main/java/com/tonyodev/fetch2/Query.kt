package com.tonyodev.fetch2

interface Query<T> {
    fun onResult(result: T?)
}
