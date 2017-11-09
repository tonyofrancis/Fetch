package com.tonyodev.fetch2

interface Disposable {
    val isDisposed: Boolean
    fun dispose()
}
