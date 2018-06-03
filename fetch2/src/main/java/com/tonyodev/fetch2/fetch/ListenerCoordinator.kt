package com.tonyodev.fetch2.fetch

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchListener

class ListenerCoordinator(val namespace: String) {

    private val lock = Any()
    private val listenerMap = mutableMapOf<Int, MutableSet<FetchListener>>()

    fun addListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val set = listenerMap[id] ?: mutableSetOf()
            set.add(fetchListener)
            listenerMap[id] = set
        }
    }

    fun removeListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val iterator = listenerMap[id]?.iterator()
            if (iterator != null) {
                while (iterator.hasNext()) {
                    val listener = iterator.next()
                    if (listener == fetchListener) {
                        iterator.remove()
                        break
                    }
                }
            }
        }
    }

    val mainListener: FetchListener = object : FetchListener {

        override fun onQueued(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.onQueued(download)
                    }
                }
            }
        }

        override fun onCompleted(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.onCompleted(download)
                    }
                }
            }
        }

        override fun onError(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.onError(download)
                    }
                }
            }
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
                    }
                }
            }
        }

        override fun onPaused(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.onPaused(download)
                    }
                }
            }
        }

        override fun onResumed(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.onResumed(download)
                    }
                }
            }
        }

        override fun onCancelled(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.onCancelled(download)
                    }
                }
            }
        }

        override fun onRemoved(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.onRemoved(download)
                    }
                }
            }
        }

        override fun onDeleted(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.onDeleted(download)
                    }
                }
            }
        }
    }

    fun clearAll() {
        listenerMap.clear()
    }

}