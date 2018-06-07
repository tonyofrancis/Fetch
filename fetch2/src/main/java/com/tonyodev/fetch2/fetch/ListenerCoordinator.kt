package com.tonyodev.fetch2.fetch

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchListener
import java.lang.ref.WeakReference

class ListenerCoordinator(val namespace: String) {

    private val lock = Any()
    private val listenerMap = mutableMapOf<Int, MutableSet<WeakReference<FetchListener>>>()

    fun addListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val set = listenerMap[id] ?: mutableSetOf()
            set.add(WeakReference(fetchListener))
            listenerMap[id] = set
        }
    }

    fun removeListener(id: Int, fetchListener: FetchListener) {
        synchronized(lock) {
            val iterator = listenerMap[id]?.iterator()
            if (iterator != null) {
                while (iterator.hasNext()) {
                    val reference = iterator.next()
                    if (reference.get() == fetchListener) {
                        iterator.remove()
                        break
                    }
                }
            }
        }
    }

    val mainListener: FetchListener = object : FetchListener {

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.get()?.onQueued(download, waitingOnNetwork)
                    }
                }
            }
        }

        override fun onCompleted(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.get()?.onCompleted(download)
                    }
                }
            }
        }

        override fun onError(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.get()?.onError(download)
                    }
                }
            }
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.get()?.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
                    }
                }
            }
        }

        override fun onPaused(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.get()?.onPaused(download)
                    }
                }
            }
        }

        override fun onResumed(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.get()?.onResumed(download)
                    }
                }
            }
        }

        override fun onCancelled(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.get()?.onCancelled(download)
                    }
                }
            }
        }

        override fun onRemoved(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.get()?.onRemoved(download)
                    }
                }
            }
        }

        override fun onDeleted(download: Download) {
            synchronized(lock) {
                listenerMap.values.forEach {
                    it.forEach {
                        it.get()?.onDeleted(download)
                    }
                }
            }
        }
    }

    fun clearAll() {
        listenerMap.clear()
    }

}