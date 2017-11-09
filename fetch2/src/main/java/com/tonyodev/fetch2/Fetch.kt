package com.tonyodev.fetch2

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v4.util.ArrayMap
import android.support.v4.util.ArraySet

import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import okhttp3.OkHttpClient


class Fetch private constructor(var context: Context, var name: String = "", var client: OkHttpClient?) : Disposable {

    private val databaseManager: DatabaseManager
    private val downloadManager: DownloadManager
    private val mainHandler: Handler
    private lateinit var executor: ExecutorService
    private val listeners: MutableSet<WeakReference<FetchListener>>
    @Volatile override var isDisposed: Boolean = false
        private set

    private val actionProcessor = object : ActionProcessor<Runnable> {

        private val queue = ConcurrentLinkedQueue<Runnable>()

        @Synchronized override fun queueAction(action: Runnable) {
            val wasEmpty = queue.isEmpty()

            queue.add(action)

            if (wasEmpty) {
                processNext()
            }
        }

        @Synchronized override fun processNext() {
            if (!executor.isShutdown && !queue.isEmpty()) {
                executor.execute(queue.remove())
            }
        }

        override fun clearQueue() {
            queue.clear()
        }
    }

    private val downloadListener: DownloadListener
        get() = object : DownloadListener {
            override fun onComplete(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long) {
                postOnMain(Runnable {
                    listeners
                            .forEach { it.get()?.onComplete(id, progress, downloadedBytes, totalBytes) }
                })
            }

            override fun onError(id: Long, error: Error, progress: Int, downloadedBytes: Long, totalBytes: Long) {
                postOnMain(Runnable {
                    listeners
                            .forEach { it.get()?.onError(id, error, progress, downloadedBytes, totalBytes) }
                })
            }

            override fun onProgress(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long) {
                postOnMain(Runnable {
                    listeners
                            .forEach { it.get()?.onProgress(id, progress, downloadedBytes, totalBytes) }
                })
            }

            override fun onPause(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long) {
                postOnMain(Runnable {
                    listeners
                            .forEach { it.get()?.onPause(id, progress, downloadedBytes, totalBytes) }
                })
            }

            override fun onCancelled(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long) {
                postOnMain(Runnable {
                    listeners
                            .forEach { it.get()?.onCancelled(id, progress, downloadedBytes, totalBytes) }
                })
            }

            override fun onRemoved(id: Long, progress: Int, downloadedBytes: Long, totalBytes: Long) {
                postOnMain(Runnable {
                    listeners
                            .forEach { it.get()?.onRemoved(id, progress, downloadedBytes, totalBytes) }
                })
            }
        }

    init {
        this.isDisposed = false
        this.listeners = ArraySet()
        this.mainHandler = Handler(Looper.getMainLooper())
        this.executor = Executors.newSingleThreadExecutor()

        if (client == null) {
            client = NetworkUtils.okHttpClient()
        }

        this.databaseManager = DatabaseManager.newInstance(context.applicationContext, this.name)
        this.downloadManager = DownloadManager.newInstance(context.applicationContext, databaseManager,
                client!!, downloadListener, actionProcessor)
    }

    @Synchronized private fun postOnMain(action: Runnable) {
        mainHandler.post(action)
    }

    fun download(request: Request): Fetch {
        FetchHelper.throwIfDisposed(this)
        FetchHelper.throwIfRequestIsNull(request)

        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : AbstractTransaction<Boolean>() {
                override fun onPreExecute() {}

                override fun onExecute(database: Database) {
                    val inserted = database.insert(request.id, request.url, request.absoluteFilePath, request.groupId)
                    value = inserted
                }

                override fun onPostExecute() {
                    if (value!!) {
                        downloadManager.resume(request.id)
                    }
                }
            })
        })
        return this
    }

    fun download(request: Request, callback: Callback) {
        FetchHelper.throwIfDisposed(this)
        FetchHelper.throwIfRequestIsNull(request)
        FetchHelper.throwIfCallbackIsNull(callback)

        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : AbstractTransaction<Boolean>() {
                override fun onPreExecute() {}

                override fun onExecute(database: Database) {

                    val inserted = database.insert(request.id, request.url, request.absoluteFilePath, request.groupId)
                    value = inserted
                }

                override fun onPostExecute() {

                    if (value!!) {
                        postOnMain(Runnable { callback.onQueued(request) })

                        downloadManager.resume(request.id)
                    } else {
                        postOnMain(Runnable { callback.onFailure(request, Error.UNKNOWN) })
                    }
                }
            })
        })
    }

    fun download(requests: List<Request>) {
        FetchHelper.throwIfDisposed(this)
        FetchHelper.throwIfRequestListIsNull(requests)

        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : AbstractTransaction<List<Long>>() {
                override fun onPreExecute() {}

                override fun onExecute(database: Database) {

                    val ids = requests
                            .filter { database.insert(it.id, it.url, it.absoluteFilePath, it.groupId) }
                            .map { it.id }

                    value = ids
                }

                override fun onPostExecute() {
                    for (id in value!!) {
                        downloadManager.resume(id)
                    }
                }
            })
        })
    }

    fun download(requests: List<Request>, callback: Callback) {
        FetchHelper.throwIfDisposed(this)
        FetchHelper.throwIfRequestListIsNull(requests)
        FetchHelper.throwIfCallbackIsNull(callback)

        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : AbstractTransaction<Map<Request, Boolean>>() {
                override fun onPreExecute() {

                }

                override fun onExecute(database: Database) {

                    val map = ArrayMap<Request, Boolean>()

                    for (request in requests) {
                        val inserted = database.insert(request.id, request.url, request.absoluteFilePath, request.groupId)
                        map.put(request, inserted)
                    }
                    value = map
                }

                override fun onPostExecute() {

                    val keys = value!!.keys

                    for (request in keys) {
                        val obtainedValue = value!![request] ?: false

                        if (obtainedValue) {

                            downloadManager.resume(request.id)

                            postOnMain(Runnable { callback.onQueued(request) })

                        } else {
                            postOnMain(Runnable { callback.onFailure(request, Error.UNKNOWN) })
                        }
                    }
                }
            })
        })
    }

    fun pause(id: Long) {
        FetchHelper.throwIfDisposed(this)

        actionProcessor.queueAction(Runnable{ downloadManager.pause(id) })
    }

    fun pauseAll() {
        FetchHelper.throwIfDisposed(this)
        actionProcessor.queueAction(Runnable{ downloadManager.pauseAll() })
    }

    fun resume(id: Long) {
        FetchHelper.throwIfDisposed(this)

        actionProcessor.queueAction(Runnable{ downloadManager.resume(id) })
    }

    fun resumeAll() {
        FetchHelper.throwIfDisposed(this)
        actionProcessor.queueAction(Runnable{ downloadManager.resumeAll() })
    }

    fun retry(id: Long) {
        FetchHelper.throwIfDisposed(this)

        actionProcessor.queueAction(Runnable{ downloadManager.retry(id) })
    }

    fun cancel(id: Long) {
        FetchHelper.throwIfDisposed(this)


        actionProcessor.queueAction(Runnable{ downloadManager.cancel(id) })
    }

    fun cancelAll() {
        FetchHelper.throwIfDisposed(this)
        actionProcessor.queueAction(Runnable{ downloadManager.cancelAll() })
    }

    fun remove(id: Long) {
        FetchHelper.throwIfDisposed(this)

        actionProcessor.queueAction(Runnable{ downloadManager.remove(id) })
    }

    fun removeAll() {
        FetchHelper.throwIfDisposed(this)
        actionProcessor.queueAction(Runnable{ downloadManager.removeAll() })
    }

    fun delete(id: Long) {
        FetchHelper.throwIfDisposed(this)
        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : AbstractTransaction<RequestData>() {
                override fun onPreExecute() {}

                override fun onExecute(database: Database) {
                    val requestData = database.query(id)
                    value = requestData
                }

                override fun onPostExecute() {
                    if (value != null) {
                        downloadManager.remove(id)
                        val file = File(value!!.absoluteFilePath)

                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
            })
        })

    }

    fun deleteAll() {
        FetchHelper.throwIfDisposed(this)
        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : AbstractTransaction<List<RequestData>>() {

                override fun onPreExecute() {

                }

                override fun onExecute(database: Database) {
                    val result = database.query()
                    value = result
                }

                override fun onPostExecute() {
                    downloadManager.removeAll()

                    if (value != null) {
                        for (requestData in value!!) {
                            val file = File(requestData.absoluteFilePath)

                            if (file.exists()) {
                                file.delete()
                            }
                        }
                    }
                }
            })
        })
    }

    fun query(id: Long, query: Query<RequestData>) {
        FetchHelper.throwIfDisposed(this)
        FetchHelper.throwIfQueryIsNull(query)

        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : Transaction {
                override fun onPreExecute() {

                }

                override fun onExecute(database: Database) {
                    val requestData = database.query(id)
                    postOnMain(Runnable { query.onResult(requestData) })
                }

                override fun onPostExecute() {

                }
            })
        })
    }

    fun query(ids: List<Long>, query: Query<List<RequestData>>) {
        FetchHelper.throwIfDisposed(this)
        FetchHelper.throwIfQueryIsNull(query)
        FetchHelper.throwIfIdListIsNull(ids)

        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : Transaction {

                override fun onPreExecute() {

                }

                override fun onExecute(database: Database) {

                    val results = database.query(FetchHelper.createIdArray(ids))
                    postOnMain(Runnable { query.onResult(results) })
                }

                override fun onPostExecute() {

                }
            })
        })
    }

    fun queryAll(query: Query<List<RequestData>>) {
        FetchHelper.throwIfDisposed(this)
        FetchHelper.throwIfQueryIsNull(query)

        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : Transaction {

                override fun onPreExecute() {

                }

                override fun onExecute(database: Database) {
                    val result = database.query()
                    postOnMain(Runnable { query.onResult(result) })
                }

                override fun onPostExecute() {

                }
            })
        })
    }

    fun queryByStatus(status: Status, query: Query<List<RequestData>>) {
        FetchHelper.throwIfDisposed(this)
        FetchHelper.throwIfQueryIsNull(query)
        FetchHelper.throwIfStatusIsNull(status)

        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : Transaction {

                override fun onPreExecute() {

                }

                override fun onExecute(database: Database) {
                    val result = database.queryByStatus(status.value)
                    postOnMain(Runnable { query.onResult(result) })
                }

                override fun onPostExecute() {

                }
            })
        })
    }

    fun queryByGroupId(groupId: String, query: Query<List<RequestData>>) {
        FetchHelper.throwIfDisposed(this)
        FetchHelper.throwIfQueryIsNull(query)
        FetchHelper.throwIfGroupIDIsNull(groupId)

        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : Transaction {
                override fun onPreExecute() {

                }

                override fun onExecute(database: Database) {
                    val result = database.queryByGroupId(groupId)
                    postOnMain(Runnable { query.onResult(result) })
                }

                override fun onPostExecute() {

                }
            })
        })
    }

    fun queryContains(id: Long, query: Query<Boolean>) {
        FetchHelper.throwIfDisposed(this)
        FetchHelper.throwIfQueryIsNull(query)

        actionProcessor.queueAction(Runnable{
            databaseManager.executeTransaction(object : Transaction {

                override fun onPreExecute() {

                }

                override fun onExecute(database: Database) {
                    val found = database.contains(id)
                    postOnMain(Runnable { query.onResult(found) })
                }

                override fun onPostExecute() {

                }
            })
        })
    }

    @Synchronized
    fun addListener(fetchListener: FetchListener) {
        FetchHelper.throwIfDisposed(this)

        if (!containsListener(fetchListener)) {
            fetchListener.onAttach(this)
            listeners.add(WeakReference(fetchListener))
        }
    }

    private fun containsListener(fetchListener: FetchListener): Boolean {
        val iterator = listeners.iterator()
        var ref: WeakReference<FetchListener>

        while (iterator.hasNext()) {
            ref = iterator.next()

            if (ref.get() != null && ref.get() === fetchListener) {
                return true
            }
        }

        return false
    }

    @Synchronized
    fun removeListener(fetchListener: FetchListener) {
        FetchHelper.throwIfDisposed(this)

        val iterator = listeners.iterator()
        var ref: WeakReference<FetchListener>

        while (iterator.hasNext()) {
            ref = iterator.next()

            if (ref.get() != null && ref.get() === fetchListener) {
                iterator.remove()
                fetchListener.onDetach(this)
                break
            }
        }
    }

    @Synchronized
    fun removeListeners() {
        FetchHelper.throwIfDisposed(this)

        val iterator = listeners.iterator()
        var ref: WeakReference<FetchListener>

        while (iterator.hasNext()) {
            ref = iterator.next()
            iterator.remove()

            ref.get()?.onDetach(this)
        }
    }

    override fun toString(): String {
        return name
    }

    @Synchronized override fun dispose() {
        if (!isDisposed) {
            removeListeners()
            executor.shutdown()
            actionProcessor.clearQueue()
            downloadManager.dispose()
            databaseManager.dispose()
            isDisposed = true
            pool.remove(name)
        }
    }

    companion object Factory {

        private val pool = ConcurrentHashMap<String, Fetch>()

        @JvmOverloads fun getDefaultInstance(context: Context, client: OkHttpClient = NetworkUtils.okHttpClient()): Fetch {

            val defaultName = FetchHelper.defaultDatabaseName
            if (pool.containsKey(defaultName)) {
                return pool[defaultName]!!
            }

            val fetch = Fetch(context, defaultName, client)
            pool.put(defaultName, fetch)
            return fetch
        }

        @JvmOverloads fun create(name: String, context: Context, client: OkHttpClient? = null): Fetch {
            if (pool.containsKey(name)) {
                return pool[name]!!
            }

            val fetch = Fetch(context, name, client)
            pool.put(name, fetch)
            return fetch
        }
    }
}