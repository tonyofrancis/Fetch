package com.tonyodev.fetch2core

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

class HandlerWrapper(val namespace: String,
                     backgroundHandler: Handler? = null) {

    private val lock = Any()
    private var closed = false
    private var usageCounter = 0
    private val handler = backgroundHandler ?: {
        val handlerThread = HandlerThread(namespace)
        handlerThread.start()
        Handler(handlerThread.looper)
    }()
    private var workerTaskHandler: Handler? = null

    fun post(runnable: () -> Unit) {
        synchronized(lock) {
            if (!closed) {
                handler.post(runnable)
            }
        }
    }

    fun postDelayed(runnable: Runnable, delayMillis: Long) {
        synchronized(lock) {
            if (!closed) {
                handler.postDelayed(runnable, delayMillis)
            }
        }
    }

    fun removeCallbacks(runnable: Runnable) {
        synchronized(lock) {
            if (!closed) {
                handler.removeCallbacks(runnable)
            }
        }
    }

    fun incrementUsageCounter() {
        synchronized(lock) {
            if (!closed) {
                usageCounter += 1
            }
        }
    }

    fun decrementUsageCounter() {
        synchronized(lock) {
            if (!closed) {
                if (usageCounter == 0) {
                    return
                }
                usageCounter -= 1
            }
        }
    }

    fun usageCount(): Int {
        return synchronized(lock) {
            if (!closed) {
                usageCounter
            } else {
                0
            }
        }
    }

    fun getLooper(): Looper {
        return synchronized(lock) {
            handler.looper
        }
    }

    fun executeWorkerTask(runnable: () -> Unit) {
        synchronized(lock) {
            if (!closed) {
                if (workerTaskHandler == null) {
                    workerTaskHandler = getNewWorkerTaskHandler()
                }
                workerTaskHandler?.post(runnable)
            }
        }
    }

    fun getWorkTaskLooper(): Looper {
        return synchronized(lock) {
            val workerHandler = workerTaskHandler
            if (workerHandler == null) {
                val newHandler = getNewWorkerTaskHandler()
                workerTaskHandler = newHandler
                newHandler.looper
            } else {
                workerHandler.looper
            }
        }
    }

    private fun getNewWorkerTaskHandler(): Handler {
        val handlerThread = HandlerThread("$namespace worker task")
        handlerThread.start()
        return Handler(handlerThread.looper)
    }

    fun close() {
        synchronized(lock) {
            if (!closed) {
                closed = true
                try {
                    handler.removeCallbacksAndMessages(null)
                    handler.looper.quit()
                } catch (e: Exception) {

                }
                try {
                    val workerHandler = workerTaskHandler
                    workerTaskHandler = null
                    workerHandler?.removeCallbacksAndMessages(null)
                    workerHandler?.looper?.quit()
                } catch (e: Exception) {

                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HandlerWrapper
        if (namespace != other.namespace) return false
        return true
    }

    override fun hashCode(): Int {
        return namespace.hashCode()
    }

}