package com.tonyodev.fetch2

import android.content.Context

import java.io.BufferedInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap

import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody


internal class DownloadManager private constructor(private val context: Context, private val databaseManager: DatabaseManager,
                                                   private val okHttpClient: OkHttpClient, private val downloadListener: DownloadListener,
                                                   private val actionProcessor: ActionProcessor<Runnable>) : Disposable {
    private val downloadsMap: ConcurrentHashMap<Long, DownloadRunnable>
    @Volatile override var isDisposed: Boolean = false
        private set

    init {
        this.isDisposed = false
        this.downloadsMap = ConcurrentHashMap()
    }

    fun pause(id: Long) {
        if (isDisposed) {
            return
        }

        interrupt(id, InterruptReason.PAUSED)
        actionProcessor.processNext()
    }

    fun pauseAll() {
        if (isDisposed) {
            return
        }

        interruptAll(InterruptReason.PAUSED)
        actionProcessor.processNext()
    }

    fun resume(id: Long) {
        if (isDisposed) {
            return
        }

        databaseManager.executeTransaction(object : Transaction {

            override fun onPreExecute() {

            }

            override fun onExecute(database: Database) {
                if (!downloadsMap.containsKey(id)) {
                    val requestData = database.query(id)
                    if (requestData != null && DownloadHelper.canRetry(requestData.status)) {
                        database.setStatusAndError(id, Status.DOWNLOADING, Error.NONE.value)
                        download(requestData)
                    }
                }
            }

            override fun onPostExecute() {

            }
        })

        actionProcessor.processNext()
    }

    fun resumeAll() {
        if (isDisposed) {
            return
        }

        databaseManager.executeTransaction(object : Transaction {

            override fun onPreExecute() {

            }

            override fun onExecute(database: Database) {
                val requestDataList = database.query()

                for (requestData in requestDataList) {

                    if (!downloadsMap.containsKey(requestData.id) && DownloadHelper.canRetry(requestData.status)) {
                        database.setStatusAndError(requestData.id, Status.DOWNLOADING, Error.NONE.value)
                        download(requestData)
                    }
                }
            }

            override fun onPostExecute() {

            }
        })

        actionProcessor.processNext()
    }

    fun retry(id: Long) {
        resume(id)
    }

    fun cancel(id: Long) {
        if (isDisposed) {
            return
        }

        databaseManager.executeTransaction(object : Transaction {

            override fun onPreExecute() {

            }

            override fun onExecute(database: Database) {
                val requestData = database.query(id)

                if (requestData != null && DownloadHelper.canCancel(requestData.status)) {

                    if (downloadsMap.containsKey(requestData.id)) {
                        interrupt(requestData.id, InterruptReason.CANCELLED)
                    } else {
                        database.setStatusAndError(id, Status.CANCELLED, Error.NONE.value)
                        downloadListener.onCancelled(id, DownloadHelper.calculateProgress(requestData.downloadedBytes, requestData.totalBytes), requestData.downloadedBytes, requestData.totalBytes)
                    }
                }
            }

            override fun onPostExecute() {

            }
        })
        actionProcessor.processNext()
    }

    fun cancelAll() {
        if (isDisposed) {
            return
        }

        databaseManager.executeTransaction(object : Transaction {

            override fun onPreExecute() {

            }

            override fun onExecute(database: Database) {
                val list = database.query()

                for (requestData in list) {

                    if (DownloadHelper.canCancel(requestData.status)) {

                        if (downloadsMap.containsKey(requestData.id)) {
                            interrupt(requestData.id, InterruptReason.CANCELLED)

                        } else {

                            database.setStatusAndError(requestData.id, Status.CANCELLED, Error.NONE.value)
                            downloadListener.onCancelled(requestData.id,
                                    DownloadHelper.calculateProgress(requestData.downloadedBytes, requestData.totalBytes), requestData.downloadedBytes, requestData.totalBytes)
                        }
                    }
                }
            }

            override fun onPostExecute() {

            }
        })
        actionProcessor.processNext()
    }

    fun remove(id: Long) {
        if (isDisposed) {
            return
        }

        if (downloadsMap.containsKey(id)) {
            interrupt(id, InterruptReason.REMOVED)
        } else {

            databaseManager.executeTransaction(object : Transaction {

                override fun onPreExecute() {

                }

                override fun onExecute(database: Database) {
                    val requestData = database.query(id)
                    if (requestData != null) {
                        database.remove(id)

                        downloadListener.onRemoved(id,
                                DownloadHelper.calculateProgress(requestData.downloadedBytes, requestData.totalBytes), requestData.downloadedBytes, requestData.totalBytes)
                    }
                }

                override fun onPostExecute() {

                }
            })
        }

        actionProcessor.processNext()
    }

    fun removeAll() {
        if (isDisposed) {
            return
        }

        databaseManager.executeTransaction(object : Transaction {

            override fun onPreExecute() {

            }

            override fun onExecute(database: Database) {
                val list = database.query()

                for (requestData in list) {

                    if (downloadsMap.containsKey(requestData.id)) {
                        interrupt(requestData.id, InterruptReason.REMOVED)

                    } else {
                        database.remove(requestData.id)
                        downloadListener.onRemoved(requestData.id,
                                DownloadHelper.calculateProgress(requestData.downloadedBytes, requestData.totalBytes), requestData.downloadedBytes, requestData.totalBytes)
                    }
                }
            }

            override fun onPostExecute() {

            }
        })

        actionProcessor.processNext()
    }

    private fun interrupt(id: Long, interruptReason: InterruptReason) {
        if (downloadsMap.containsKey(id)) {
            val downloadRunnable = downloadsMap[id]

            if (downloadRunnable != null) {
                downloadRunnable.interrupt(interruptReason)

                try {
                    downloadRunnable.thread!!.join(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun interruptAll(reason: InterruptReason) {
        val keys = downloadsMap.keys

        for (key in keys) {

            val downloadRunnable = downloadsMap[key]

            if (downloadRunnable != null) {
                downloadRunnable.interrupt(reason)

                try {
                    downloadRunnable.thread!!.join(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun download(requestData: RequestData?) {
        if (requestData == null || downloadsMap.containsKey(requestData.id)) {
            return
        }

        val downloadRunnable = DownloadRunnable(requestData)
        val thread = Thread(downloadRunnable)
        downloadRunnable.thread = thread
        downloadsMap.put(requestData.id, downloadRunnable)
        thread.start()
    }

    private inner class DownloadRunnable internal constructor(private val request: RequestData) : Runnable {
        @Volatile internal var isInterrupted: Boolean = false
            private set
        private var interruptReason: InterruptReason? = null
        internal var thread: Thread? = null

        internal var response: Response? = null
        internal var body: ResponseBody? = null
        internal var input: BufferedInputStream? = null
        internal var output: RandomAccessFile? = null
        internal var downloadedBytes = 0L
        internal var totalBytes = 0L
        internal var progress = 0


        init {
            this.isInterrupted = false
        }

        internal fun interrupt(reason: InterruptReason) {
            if (isInterrupted) {
                return
            }
            interruptReason = reason
            isInterrupted = true
        }

        override fun run() {

            val oldThreadName = thread!!.name
            thread!!.name = "DownloaderThread url:" + request.url

            try {

                val file = DownloadHelper.createFileOrThrow(request.absoluteFilePath)
                downloadedBytes = file.length()
                totalBytes = request.totalBytes
                progress = DownloadHelper.calculateProgress(totalBytes, downloadedBytes)

                if (!isInterrupted) {

                    val call = okHttpClient.newCall(DownloadHelper.createHttpRequest(request))
                    response = call.execute()
                    body = response!!.body()

                    if (response!!.isSuccessful && body != null && !isInterrupted) {

                        totalBytes = downloadedBytes + java.lang.Long.valueOf(response!!.header("Content-Length"))!!

                        databaseManager.executeTransaction(object : Transaction {

                            override fun onPreExecute() {

                            }

                            override fun onExecute(database: Database) {
                                database.setDownloadedBytesAndTotalBytes(request.id, downloadedBytes, totalBytes)
                            }

                            override fun onPostExecute() {

                            }
                        })

                        input = BufferedInputStream(body!!.byteStream())
                        output = RandomAccessFile(request.absoluteFilePath, "rw")

                        if (response!!.code() == HttpURLConnection.HTTP_PARTIAL) {
                            output!!.seek(downloadedBytes)
                        } else {
                            output!!.seek(0)
                        }

                        val buffer = ByteArray(1024)
                        var read: Int
                        var startTime: Long
                        var stopTime: Long

                        startTime = System.nanoTime()
                        read = input!!.read(buffer, 0, 1024)

                        while (read != -1 && !isInterrupted) {
                            output!!.write(buffer, 0, read)
                            downloadedBytes += read.toLong()

                            databaseManager.executeTransaction(object : Transaction {

                                override fun onPreExecute() {

                                }

                                override fun onExecute(database: Database) {
                                    database.updateDownloadedBytes(request.id, downloadedBytes)
                                }

                                override fun onPostExecute() {

                                }
                            })

                            progress = DownloadHelper.calculateProgress(downloadedBytes, totalBytes)

                            stopTime = System.nanoTime()
                            if (DownloadHelper.hasTwoSecondsPassed(startTime, stopTime)) {
                                downloadListener.onProgress(request.id, progress, downloadedBytes, totalBytes)
                                startTime = System.nanoTime()
                            }

                            read = input!!.read(buffer, 0, 1024)
                        }
                    } else if (!response!!.isSuccessful) {
                        throw IOException("invalid server response")
                    }
                }

                databaseManager.executeTransaction(object : Transaction {

                    override fun onPreExecute() {

                    }

                    override fun onExecute(database: Database) {
                        database.setDownloadedBytesAndTotalBytes(request.id, downloadedBytes, totalBytes)
                    }

                    override fun onPostExecute() {

                    }
                })

                progress = DownloadHelper.calculateProgress(downloadedBytes, totalBytes)
                downloadListener.onProgress(request.id, progress, downloadedBytes, totalBytes)

                if (!isInterrupted) {

                    databaseManager.executeTransaction(object : Transaction {

                        override fun onPreExecute() {

                        }

                        override fun onExecute(database: Database) {
                            database.setStatusAndError(request.id, Status.COMPLETED, Error.NONE.value)
                        }

                        override fun onPostExecute() {

                        }
                    })

                    downloadListener.onComplete(request.id, progress, downloadedBytes, totalBytes)
                } else {
                    when (interruptReason) {
                        InterruptReason.PAUSED -> {

                            databaseManager.executeTransaction(object : Transaction {

                                override fun onPreExecute() {

                                }

                                override fun onExecute(database: Database) {
                                    database.setStatusAndError(request.id, Status.PAUSED, Error.NONE.value)
                                }

                                override fun onPostExecute() {

                                }
                            })

                            downloadListener.onPause(request.id, progress, downloadedBytes, totalBytes)
                        }
                        InterruptReason.CANCELLED -> {

                            databaseManager.executeTransaction(object : Transaction {

                                override fun onPreExecute() {

                                }

                                override fun onExecute(database: Database) {
                                    database.setStatusAndError(request.id, Status.CANCELLED, Error.NONE.value)
                                }

                                override fun onPostExecute() {

                                }
                            })

                            downloadListener.onCancelled(request.id, progress, downloadedBytes, totalBytes)
                        }
                        InterruptReason.REMOVED -> {

                            databaseManager.executeTransaction(object : Transaction {

                                override fun onPreExecute() {

                                }

                                override fun onExecute(database: Database) {
                                    database.remove(request.id)
                                }

                                override fun onPostExecute() {

                                }
                            })

                            downloadListener.onRemoved(request.id, progress, downloadedBytes, totalBytes)
                        }
                    }
                }
            } catch (e: Exception) {
                val reason = ErrorUtils.getCode(e.message)

                if (!NetworkUtils.isNetworkAvailable(context) && reason == Error.HTTP_NOT_FOUND) {

                    databaseManager.executeTransaction(object : Transaction {

                        override fun onPreExecute() {

                        }

                        override fun onExecute(database: Database) {
                            database.setStatusAndError(request.id, Status.ERROR, Error.NO_NETWORK_CONNECTION.value)
                        }

                        override fun onPostExecute() {

                        }
                    })

                } else {
                    databaseManager.executeTransaction(object : Transaction {

                        override fun onPreExecute() {

                        }

                        override fun onExecute(database: Database) {
                            database.setStatusAndError(request.id, Status.ERROR, reason.value)
                        }

                        override fun onPostExecute() {

                        }
                    })
                }

                downloadListener.onError(request.id, reason, progress, downloadedBytes, totalBytes)
            } finally {
                downloadsMap.remove(request.id)

                if (response != null) {
                    response!!.close()
                }
                if (body != null) {
                    body!!.close()
                }
                if (output != null) {
                    try {
                        output!!.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
                if (input != null) {
                    try {
                        input!!.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }

                thread!!.name = oldThreadName
            }
        }
    }

    internal enum class InterruptReason {
        PAUSED,
        CANCELLED,
        REMOVED
    }

    @Synchronized override fun dispose() {
        if (!isDisposed) {
            pauseAll()
            isDisposed = true
        }
    }

    companion object {

        fun newInstance(context: Context, databaseManager: DatabaseManager,
                        client: OkHttpClient, downloadListener: DownloadListener, actionProcessor: ActionProcessor<Runnable>): DownloadManager {
            return DownloadManager(context, databaseManager, client, downloadListener, actionProcessor)
        }
    }
}
