package com.tonyodev.fetch2.fetch

class LiveSettings(val namespace: String) {

    private val lock = Any()

    @Volatile
    var didSanitizeDatabaseOnFirstEntry: Boolean = false

    fun execute(func: (LiveSettings) -> Unit) {
        synchronized(lock) {
            func(this)
        }
    }

}